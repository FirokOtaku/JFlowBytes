package firok.spring.jfb.flow;

import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service_impl.ContextKeys;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

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
		return Objects.equals(typeCurrent, typeRequire);
	}

	// todo 日志功能以后再做 现在没时间做日志
	private final List<LogNode> listLog = new ArrayList<>(20);
	private static class LogNode { Level level; String message; long time; public String toString() { return time + "|" + level + ": " + message; } }
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

		System.out.printf("[WF|%d|%s|%s]\n", now, this.id, msg);
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
	 * @deprecated 暂时没想好这个要不要这么处理
	 * todo 可能会移除掉
	 */
	@Deprecated
	public WorkflowThread thread;
}
