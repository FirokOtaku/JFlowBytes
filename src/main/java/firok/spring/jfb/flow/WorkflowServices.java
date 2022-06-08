package firok.spring.jfb.flow;

import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class WorkflowServices implements ApplicationContextAware
{
	private boolean hasFlowContextInit = false;
	private final Map<String, IWorkflowService> mapService = new HashMap<>();
	private final Set<String> setServiceName = new HashSet<>();
	/**
	 * spring 上下文, 用于寻找可用的 flow 服务实例
	 */
	ApplicationContext context;
	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException
	{
		this.context = context;
	}

	/**
	 * 扫描 Spring 上下文, 获取所有工作流处理器
	 */
	@PostConstruct
	public void scanWorkflowServices() throws ExceptionIntegrative
	{
		synchronized (this)
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

	public IWorkflowService getService(String nameService)
	{
		Objects.requireNonNull(nameService, "名称不可为空");
		synchronized (this)
		{
			return mapService.get(nameService);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> getServicesOf(Class<T> clasz)
	{
		Objects.requireNonNull(clasz, "类型不可为空");
		var ret = new ArrayList<T>();
		synchronized (this)
		{
			for(var service : mapService.values())
			{
				if(clasz.isInstance(service))
				{
					ret.add((T) service);
				}
			}
		}
		return ret;
	}

	public List<String> getServiceNames()
	{
		return new ArrayList<>(setServiceName);
	}

	@Value("${app.flow.log-console}")
	public boolean isLogConsole;

	public static WorkflowServices instance;
	public WorkflowServices()
	{
		WorkflowServices.instance = this;
	}

	/**
	 * 停止并清理工作流
	 * @param context 工作流上下文
	 * @param interrupt 是否打断线程
	 * @param cleanCache 是否清理缓存
	 * @throws Exception 发生任何异常
	 */
	public static void cleanWorkflow(WorkflowContext context, boolean interrupt, boolean cleanCache) throws Exception
	{
		if(interrupt)
			context.thread.interrupt();
		if(cleanCache)
			FileUtils.forceDelete(context.folderWorkflowRoot);
	}
}
