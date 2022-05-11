package firok.spring.jfb.bean;

import firok.spring.jfb.service.IWorkflowService;

import java.io.File;
import java.util.*;

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

	/**
	 * 当前正在进行的操作
	 */
	IWorkflowService currentOperation;

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
		this.listOperation = listOperation;
		this.currentOperation = listOperation.isEmpty() ? null : listOperation.get(0);
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
}
