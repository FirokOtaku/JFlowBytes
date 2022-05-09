package firok.spring.jfb.controller;

import firok.spring.jfb.bean.Ret;
import firok.spring.jfb.bean.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;

@RestController
public class FlowController implements ApplicationContextAware
{
	private boolean hasFlowContextInit = false;
	private final Map<String, IWorkflowService> mapService = new HashMap<>();
	private final Set<String> setServiceName = new HashSet<>();

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
	 * 创建一个从上传文件开始的工作流
	 */
	@PostMapping("/api/workflow/create_workflow_start_from_upload")
	public Ret<?> createWorkflowStartFromUpload(
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



		return Ret.success();
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
