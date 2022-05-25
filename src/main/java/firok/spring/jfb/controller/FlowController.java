package firok.spring.jfb.controller;

import firok.spring.jfb.bean.Ret;
import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.flow.WorkflowThread;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.upload.IUploadIntegrative;
import lombok.Data;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/api/workflow")
public class FlowController implements ApplicationContextAware
{
	private boolean hasFlowContextInit = false;
	private final Map<String, IWorkflowService> mapService = new HashMap<>();
	private final Set<String> setServiceName = new HashSet<>();

	private final Object LOCK_WORKFLOW = new Object();
	private final Map<String, WorkflowContext> mapWorkflow = new HashMap<>();

	@PreDestroy
	void preDestroy()
	{
		System.out.println("开始清理工作流");
		int countSuccess = 0, countFail = 0;
		synchronized (LOCK_WORKFLOW)
		{
			for(var workflow : mapWorkflow.values())
			{
				try
				{
					stopWorkflow(workflow);
					countSuccess++;
				}
				// todo 有空再说吧
				catch (Exception ignored)
				{
					countFail++;
				}
			}
		}
		System.out.printf("工作流清理完成. 成功 %d 个, 失败 %d 个\n", countSuccess, countFail);
	}

	/**
	 * 扫描 Spring 上下文, 获取所有工作流处理器
	 */
	@PostConstruct
	public void scanWorkflowServices() throws ExceptionIntegrative
	{
		synchronized (LOCK_WORKFLOW)
		{
			if(hasFlowContextInit) return;

			var mapServiceContext = context.getBeansOfType(IWorkflowService.class, true, true);

			// 注册所有上下文提供的工作流处理器
			for(var entryServiceContext : mapServiceContext.entrySet())
			{
				IWorkflowService serviceContext = entryServiceContext.getValue();
				String nameServiceContext = serviceContext.getWorkflowServiceOperation();

				if(setServiceName.contains(nameServiceContext))
					throw new ExceptionIntegrative("无法注册重复的工作流处理器: "+nameServiceContext);

				setServiceName.add(nameServiceContext);
				mapService.put(nameServiceContext, serviceContext);
			}

			hasFlowContextInit = true;
		}
	}

	protected IWorkflowService getService(String nameService)
	{
		synchronized (this)
		{
			return mapService.get(nameService);
		}
	}

	@Value("${app.flow.folder-flow}")
	File folderWorkflow;

	/**
	 * 创建一个工作流
	 */
	@PostMapping("/create_workflow")
	public Ret<?> createWorkflow(
			@RequestBody CreateWorkflowParam param
	)
	{
		var listNameOperation = param.getListOperation();
		var mapContextInitParam = param.getMapContextInitParam();

		var listService = new ArrayList<IWorkflowService>();
		for(var nameOperation : listNameOperation)
		{
			var service = getService(nameOperation);
			if(service == null)
				return Ret.fail("找不到指定工作流处理器: "+nameOperation);
			listService.add(service);
		}

		var id = UUID.randomUUID().toString();
		var folderWorkflowRoot = new File(folderWorkflow, id);
		var context = new WorkflowContext(id, listService, mapContextInitParam, folderWorkflowRoot);
		var thread = new WorkflowThread(context);
		context.thread = thread;
		synchronized (LOCK_WORKFLOW)
		{
			mapWorkflow.put(context.id, context);
		}
		thread.start();
		return Ret.success(id);
	}
	@GetMapping("/list_current_workflow")
	public Ret<?> listCurrentWorkflows()
	{
		var ret = new HashMap<String, String>();
		synchronized (LOCK_WORKFLOW)
		{
			for(var entryWorkflow : mapWorkflow.entrySet())
			{
				var idWorkflow = entryWorkflow.getKey();
				var workflow = entryWorkflow.getValue();
				String currentOperation;
				synchronized (workflow.LOCK)
				{
					currentOperation = workflow.getCurrentOperationName();
				}
				ret.put(idWorkflow, currentOperation);
			}
		}
		return Ret.success(ret);
	}

	@DeleteMapping("/delete_workflow")
	public Ret<?> deleteWorkflow(
			@RequestParam("idWorkflow") String idWorkflow
	)
	{
		WorkflowContext workflow;
		synchronized (LOCK_WORKFLOW)
		{
			workflow = mapWorkflow.remove(idWorkflow);
		}
		if(workflow == null)
			return Ret.fail("找不到指定工作流: "+idWorkflow);

		try // 清理工作流目录
		{
			stopWorkflow(workflow);
			return Ret.success();
		}
		catch (Exception e)
		{
			return Ret.fail("工作流已移除自队列, 但清理时工作目录时发生错误. 这通常不影响系统运行, 但可能需要手动清理: " + workflow.id);
		}
	}

	private void stopWorkflow(WorkflowContext context) throws Exception
	{
		context.thread.interrupt();
		FileUtils.forceDelete(context.folderWorkflowRoot);
	}

	/**
	 * 特殊接口, 向某个进行中的工作流上传一个文件分片
	 * @param fileSlice 分片文件
	 * @return boolean[] 返回文件分片上传状态
	 */
	@PostMapping("/service_upload_slice")
	public Ret<?> workflow_uploadSlice(
			@RequestParam("idWorkflow") String idWorkflow,
			@RequestParam("indexSlice") Integer indexSlice,
			@RequestPart("file") MultipartFile fileSlice
	)
	{
		WorkflowContext context;
		// 检查是否存在指定工作流
		synchronized (LOCK_WORKFLOW)
		{
			context = mapWorkflow.get(idWorkflow);
		}
		if(context == null)
			return Ret.fail("找不到指定工作流");

		// 检查指定工作流状态是否为等待上传中
		IUploadIntegrative serviceUpload;
		synchronized (context.LOCK)
		{
			serviceUpload = context.getCurrentOperation() instanceof IUploadIntegrative service ? service : null;
		}
		if(serviceUpload == null)
			return Ret.fail("指定工作流当前不接受文件分片上传");

		// 开始转移数据
		try
		{
			var status = serviceUpload.uploadSlice(context, indexSlice, fileSlice);
			return Ret.success(status);
		}
		catch (ExceptionIntegrative e)
		{
			return Ret.fail(e.getMessage());
		}
	}

	@Data
	public static class CreateWorkflowParam
	{
		/**
		 * 需要进行的操作
		 */
		String[] listOperation;

		/**
		 * 上下文初始参数
		 */
		Map<String, ?> mapContextInitParam;
	}

	/**
	 * spring 上下文, 用于寻找可用的 flow 服务实例
	 */
	ApplicationContext context;
	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException
	{
		this.context = context;
	}

	@GetMapping("/list_workflow_service")
	public Ret<Collection<String>> listWorkflowService()
	{
		List<String> list;
		synchronized (LOCK_WORKFLOW)
		{
			list = new ArrayList<>(setServiceName);
		}
		return Ret.success(list);
	}
}
