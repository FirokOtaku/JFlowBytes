package firok.spring.jfb.service;

import firok.spring.jfb.bean.WorkflowContext;

import java.util.HashMap;
import java.util.Map;

/**
 * 标明某个 service 支持工作流化调用
 */
public interface IWorkflowService
{
	/**
	 * @return 此工作流的操作类型名称, 需全局唯一, 否则在初始化工作流管理器时会报错
	 * @implNote 不能返回 null
	 */
	String getWorkflowServiceOperation();

	/**
	 * @return 此工作流想要正确开始操作时, 所需上下文内包含的参数
	 * @implNote 不能返回 null
	 */
	default Map<String, Class<?>> getWorkflowParamContext()
	{
		return new HashMap<>();
	}

	/**
	 * 判断某个上下文是否允许开始此操作
	 * @param context 工作流上下文
	 * @return 工作流上下文是否允许开始此操作
	 * @implNote 此方法不应对 @{@code #getWorkflowParamContext() } 中数据做出修改
	 */
	default boolean isWorkflowContextSuitable(WorkflowContext context)
	{
		var mapParam = getWorkflowParamContext();
		for(var entryParam : mapParam.entrySet())
		{
			var nameParam = entryParam.getKey();
			var typeParam = entryParam.getValue();
			if(!context.hasParamOfType(nameParam, typeParam))
				return false;
		}
		return true;
	}

	/**
	 * 开始操作工作流上下文
	 * @param context 工作流上下文
	 * @throws ExceptionIntegrative 发生任何错误时都可抛出, 此时工作流管理器会视为操作失败
	 */
	void operateWorkflow(WorkflowContext context) throws ExceptionIntegrative;

	/**
	 * 清理工作流上下文. 无论此操作成功完成还是失败完成, 都会立刻调用此方法`
	 * @param context 工作流上下文
	 * @param isSuccess 本次清理操作是否发生于操作成功后
	 * @throws ExceptionIntegrative 发生任何错误时都可抛出. 根据配置不同, 清理操作发生错误可能会导致任务失败
	 */
	void cleanWorkflow(WorkflowContext context, boolean isSuccess) throws ExceptionIntegrative;
}
