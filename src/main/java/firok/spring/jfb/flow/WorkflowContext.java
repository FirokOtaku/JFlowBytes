package firok.spring.jfb.flow;

import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.constant.ContextKeys;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class WorkflowContext extends HashMap<String, Object>
{
	/**
	 * 线程数据锁 所有对上下文数据的读写操作都需要对此加锁
	 */
	public final Object LOCK = new Object();

	/**
	 * 流程唯一id
	 */
	public final String id;

	/**
	 * 要进行的操作
	 */
	List<IWorkflowService> listOperation;

	public IWorkflowService getCurrentOperation()
	{
		return this.currentOperationIndex >= 0 && this.currentOperationIndex < this.listOperation.size() ?
				this.listOperation.get(this.currentOperationIndex) : null;
	}
	public String getCurrentOperationName()
	{
		if(this.currentOperationIndex == Integer.MIN_VALUE) return ContextKeys.CONTEXT_NOT_STARTED;
		else if(this.currentOperationIndex == Integer.MAX_VALUE) return ContextKeys.CONTEXT_FINISHED;
		else return Optional.ofNullable(this.getCurrentOperation())
				.map(IWorkflowService::getWorkflowServiceOperation)
				.orElse(ContextKeys.CONTEXT_UNKNOWN);
	}

	public boolean hasNextOperation()
	{
		return this.currentOperationIndex <= this.listOperation.size();
	}

	public void nextOperation()
	{
		if(currentOperationIndex == Integer.MIN_VALUE)
			currentOperationIndex = 0;
		else
			this.currentOperationIndex ++;
	}

	/**
	 * 当前正在进行的操作序列号
	 * Integer.MIN_VALUE 表示未开始
	 * Integer.MAX_VALUE 表示已结束
	 * 其它数字表示正在进行的操作序列号
	 */
	int currentOperationIndex;

	/**
	 * 此上下文相关根路径, 用于存放各种缓存数据
	 */
	public final File folderWorkflowRoot;

	public WorkflowContext(
			String id,
			List<IWorkflowService> listOperation,
			Map<String, ?> mapContextInitParam,
			File folderWorkflowRoot
	)
	{
		super();
		this.id = id;
		this.listOperation = new ArrayList<>(listOperation);
		this.currentOperationIndex = Integer.MIN_VALUE;
		this.putAll(mapContextInitParam);
		this.folderWorkflowRoot = folderWorkflowRoot;
	}


	/**
	 * @param nameParam 参数名
	 * @return 参数类型
	 */
	public Class<?> getParamClass(String nameParam)
	{
		var param = this.get(nameParam);
		return param == null ? null : param.getClass();
	}

	/**
	 * @param nameParam 参数名
	 * @param typeParam 参数类型
	 * @return 当前工作流上下文内, 指定参数是否为指定类型
	 */
	public boolean hasParamOfType(String nameParam, Class<?> typeParam)
	{
		var typeCurrent = getParamClass(nameParam);
		Class<?> typeRequire;
		// 这么写的原因就是想用原始值类型的class
		// 虽然未来的 Java 版本中值类型可能会取消就是了
		if(typeParam == byte.class) typeRequire = Byte.class;
		else if(typeParam == int.class) typeRequire = Integer.class;
		else if(typeParam == short.class) typeRequire = Short.class;
		else if(typeParam == long.class) typeRequire = Long.class;
		else if(typeParam == float.class) typeRequire = Float.class;
		else if(typeParam == double.class) typeRequire = Double.class;
		else if(typeParam == char.class) typeRequire = Character.class;
		else if(typeParam == boolean.class) typeRequire = Boolean.class;
		// void.class 类型用来标记不需要指定参数
		// 那谁知道这代码有没有用呢
		// 那这系统有没有用都是问题
		else if(typeParam == void.class) typeRequire = null;
		else typeRequire = typeParam;
		return typeRequire != null && typeCurrent != null && typeRequire.isAssignableFrom(typeCurrent);
	}

	private final List<LogNode> listLog = new ArrayList<>(20);

	/**
	 * 获取工作流当前状态
	 * @param lenLog 包含最近几条日志
	 */
	public WorkflowStatus getCurrentStatus(int lenLog)
	{
		var ret = new WorkflowStatus();
		ret.id = this.id;

		// 日志信息列表
		var lenLogNow = listLog.size();
		if(lenLog > 0 && lenLogNow > 0)
		{
			if(lenLog >= lenLogNow)
			{
				ret.listLog = new ArrayList<>(listLog);
			}
			else
			{
				var start = Math.max(0, lenLogNow - lenLog);
				ret.listLog = listLog.subList(start, start + lenLog);
			}
		}
		ret.sizeLog = lenLogNow;

		// 处理器列表
		ret.listOperationName = listOperation.stream()
				.map(IWorkflowService::getWorkflowServiceOperation)
				.collect(Collectors.toList());

		var service = getCurrentOperation();
		// 当前处理器名称
		ret.currentOperationName = getCurrentOperationName();
		// 当前处理器进度
		ret.currentOperationProgressTotal = service == null ? 1 : service.getMaxProgress(this);
		ret.currentOperationProgressNow = service == null ? 0 : service.getNowProgress(this);

		// 当前工作流是否出错
		ret.isError = exception != null;

		return ret;
	}

	@SuppressWarnings("deprecation")
	public void log(Level level, Object obj)
	{
		if(obj == null) return;
		var now = System.currentTimeMillis();
		var msg = String.valueOf(obj);

		var node = new LogNode();
		node.level = level;
		node.message = msg;
		node.time = now;
		synchronized (listLog)
		{
			listLog.add(node);
		}

		if(WorkflowServices.instance.isLogConsole)
			System.out.printf("[%s] {%s} %s\n", new Date(now).toLocaleString(), this.id, msg);
	}
	public String contentLog()
	{
		var sb = new StringBuilder();

		for(var log : listLog)
			sb.append(log);

		return sb.toString();
	}

	/**
	 * 工作流相关线程
	 */
	public WorkflowThread thread;

	/**
	 * 当前工作流上下文的异常信息
	 * 如果不为空 则表示当前工作流上下文出现异常
	 * 此工作流不会继续进行
	 * 等待前台处理 或后台做超时删除
	 */
	public Throwable exception;
}
