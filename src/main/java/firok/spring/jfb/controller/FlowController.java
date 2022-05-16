package firok.spring.jfb.controller;

import firok.spring.jfb.bean.Ret;
import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.flow.WorkflowThread;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.upload.IUploadIntegrative;
import firok.spring.jfb.service_impl.ContextKeys;
import firok.spring.jfb.service_impl.upload.UploadService;
import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
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
	private final Map<String, WorkflowContext> listWorkflow = new HashMap<>();

	@PostConstruct
	public void postConstruct() throws ExceptionIntegrative
	{
		synchronized (this)
		{
			if(hasFlowContextInit) return;

			var mapServiceContext = context.getBeansOfType(IWorkflowService.class);

			// 注册所有上下文提供的工作流处理器
			for(var entryServiceContext : mapServiceContext.entrySet())
			{
				String nameServiceContext = entryServiceContext.getKey();
				IWorkflowService serviceContext = entryServiceContext.getValue();

				if(setServiceName.contains(nameServiceContext))
					throw new ExceptionIntegrative("无法注册重复的工作流处理器: "+nameServiceContext);

				setServiceName.add(nameServiceContext);
				mapServiceContext.put(nameServiceContext, serviceContext);
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

	@Value("${folder-flow-root}")
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
			listWorkflow.put(context.id, context);
		}
		thread.start();
		return Ret.success(id);
	}

	/**
	 * 特殊接口, 向某个进行中的工作流上传一个文件分片
	 * @param fileSlice 分片文件
	 * @return boolean[] 返回文件分片上传状态
	 */
	@PostMapping("/service_upload_slice")
	public Ret<?> workflow_uploadSlice(
			@RequestParam("idWorkflow") String idWorkflow,
			@RequestParam("locSlice") Integer locSlice,
			MultipartFile fileSlice
	)
	{
		WorkflowContext context;
		// 检查是否存在指定工作流
		synchronized (LOCK_WORKFLOW)
		{
			context = listWorkflow.get(idWorkflow);
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
			var status = serviceUpload.uploadSlice(context, locSlice, fileSlice);
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
}
