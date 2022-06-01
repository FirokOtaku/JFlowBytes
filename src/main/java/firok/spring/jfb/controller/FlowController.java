package firok.spring.jfb.controller;

import firok.spring.jfb.bean.Ret;
import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.flow.WorkflowServices;
import firok.spring.jfb.flow.WorkflowStatus;
import firok.spring.jfb.flow.WorkflowThread;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.upload.IUploadIntegrative;
import lombok.Data;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PreDestroy;
import java.io.File;
import java.util.*;

/**
 * 工作流相关接口
 * */
@RestController
@RequestMapping("/api/workflow")
public class FlowController
{
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

	@Value("${app.flow.folder-flow}")
	File folderWorkflow;

	@Autowired
	WorkflowServices services;

	/**
	 * 创建一个工作流
	 * */
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
			var service = services.getService(nameOperation);
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

	/**
	 * 获取当前工作流列表
	 */
	@GetMapping("/list_current_workflow")
	public Ret<?> listCurrentWorkflows()
	{
		var ret = new HashMap<String, WorkflowStatus>();
		synchronized (LOCK_WORKFLOW)
		{
			for(var entryWorkflow : mapWorkflow.entrySet())
			{
				var idWorkflow = entryWorkflow.getKey();
				var workflow = entryWorkflow.getValue();
				WorkflowStatus status;
				synchronized (workflow.LOCK)
				{
					status = workflow.getCurrentStatus(3);
				}
				ret.put(idWorkflow, status);
			}
		}
		return Ret.success(ret);
	}

	/**
	 * 根据工作流id停止并删除指定工作流
	 * */
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
	 * */
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

	/**
	 * 创建工作流参数
	 * */
	@Data
	public static class CreateWorkflowParam
	{
		/**
		 * 工作流处理器列表
		 * */
		String[] listOperation;

		/**
		 * 工作流上下文初始参数
		 * */
		Map<String, ?> mapContextInitParam;
	}


	/**
	 * 获取可用的工作流处理器列表
	 */
	@GetMapping("/list_workflow_service")
	public Ret<Collection<String>> listWorkflowService()
	{
		List<String> list;
		synchronized (LOCK_WORKFLOW)
		{
			list = new ArrayList<>(services.getServiceNames());
		}
		return Ret.success(list);
	}
}
