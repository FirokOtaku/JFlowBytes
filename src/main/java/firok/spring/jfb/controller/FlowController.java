package firok.spring.jfb.controller;

import firok.spring.jfb.bean.Ret;
import firok.spring.jfb.constant.ContextKeys;
import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.flow.WorkflowServices;
import firok.spring.jfb.flow.WorkflowStatus;
import firok.spring.jfb.flow.WorkflowThread;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.upload.IUploadIntegrative;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 工作流相关接口
 * */
@RestController
@RequestMapping("/api/workflow")
@CrossOrigin(origins = "*")
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
					WorkflowServices.cleanWorkflow(workflow, true, true);
					countSuccess++;
				}
				catch (FileNotFoundException e)
				{
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

	public record ListCurrentWorkflowParam(
			Set<String> listWorkflowId,
			Integer lenLog
	) { }

	/**
	 * 获取当前工作流列表
	 */
	@PostMapping("/list_current_workflow")
	public Ret<?> listCurrentWorkflows(
			@RequestBody(required = false) ListCurrentWorkflowParam params
	)
	{
		var ret = new HashMap<String, WorkflowStatus>();
		// 整理前台传参
		var listId = params != null && params.listWorkflowId() != null ? params.listWorkflowId() : null;
		var lenLog = params != null && params.lenLog() != null ? params.lenLog() : 3;
		if(lenLog < 0) lenLog = 0;
		if(lenLog > 999) lenLog = 999;
		final long now = System.currentTimeMillis();
		final long limit = now + 300_000;
		// 开始查询
		synchronized (LOCK_WORKFLOW)
		{
			for(var entryWorkflow : mapWorkflow.entrySet())
			{
				var idWorkflow = entryWorkflow.getKey();
				if(listId != null && !listId.contains(idWorkflow)) // 不包含此工作流
					continue;

				var workflow = entryWorkflow.getValue();
				WorkflowStatus status;
				synchronized (workflow.LOCK)
				{
					status = workflow.getCurrentStatus(lenLog);
					// 每次查询到工作流信息 相当于ping一次工作流 都会把这条工作流的超时时间延后
					if(workflow.get(ContextKeys.KEY_TIME_TIMEOUT_LIMIT) instanceof Long)
						workflow.put(ContextKeys.KEY_TIME_TIMEOUT_LIMIT, limit);
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
			WorkflowServices.cleanWorkflow(workflow, true, true);
			return Ret.success();
		}
		catch (Exception e)
		{
			return Ret.success("工作流已移除自队列, 但清理时工作目录时发生错误. 这通常不影响系统运行, 但可能需要手动清理: " + workflow.id);
		}
	}

	/**
	 * 由于超时原因清理工作流
	 * @param idWorkflow 工作流id
	 * @throws Exception 清理异常
	 */
	void deleteWorkflowByTimeout(String idWorkflow) throws Exception
	{

	}

	/**
	 * 检查并清理超时的工作流
	 */
	public void cleanTimeoutWorkflow()
	{
		var listWorkflow = new ArrayList<WorkflowContext>();
		int countActive = 0;
		synchronized (LOCK_WORKFLOW)
		{
			long now = System.currentTimeMillis();

			var list = new ArrayList<String>();
			for(var entry : mapWorkflow.entrySet())
			{
				var id = entry.getKey();
				var context = entry.getValue();
				var timeTimeoutLimit = context.get(ContextKeys.KEY_TIME_TIMEOUT_LIMIT) instanceof Long time ? time : 0;

				boolean isTimeout;
				if(timeTimeoutLimit > 0) // 存在超时期限 绕过处理器判断机制
				{
					isTimeout = now - timeTimeoutLimit > 0;
				}
				else // 没有设定超时期限 交付处理器判断
				{
					var service = context.getCurrentOperation();
					isTimeout = (service == null) ||
							(service.shouldCheckTimeout(context, now) && service.isTimeout(context, now));
				}

				if (isTimeout) list.add(id);
				else countActive++;
			}

			for(var id : list)
			{
				var workflow = mapWorkflow.remove(id);
				listWorkflow.add(workflow);
			}
		}

		final AtomicInteger countSuccess = new AtomicInteger(0), countFail = new AtomicInteger(0);
		listWorkflow.parallelStream().forEach(workflow ->
		{
			try
			{
				WorkflowServices.cleanWorkflow(workflow, true, true);
				countSuccess.incrementAndGet();
			}
			catch (FileNotFoundException e)
			{
				countSuccess.incrementAndGet();
			}
			catch (Exception e)
			{
				countFail.incrementAndGet();
			}
		});

		if(services.isLogConsole)
		{
			System.out.printf(
					"[%s] 清理超时工作流: 成功 %d 个, 失败 %d 个. 剩余工作流: 活跃 %d 个.\n",
					new Date().toLocaleString(),
					countSuccess.get(), countFail.get(), countActive
			);
		}
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
