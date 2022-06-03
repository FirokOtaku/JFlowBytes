package firok.spring.jfb.flow;

import com.fasterxml.jackson.annotation.JsonGetter;
import firok.spring.jfb.constant.ContextKeys;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

/**
 * 工作流状态, 用来给前台展示信息用的
 */
@Getter
public class WorkflowStatus
{
	/**
	 * 工作流id
	 */
	String id;

	/**
	 * 最近几条日志
	 */
	List<LogNode> listLog;
	/**
	 * 总共有多少日志
	 */
	int sizeLog;

	/**
	 * 操作名称列表
	 */
	List<String> listOperationName;

	/**
	 * 当前处理器名称
	 */
	String currentOperationName;

	/**
	 * 当前处理器总进度
	 */
	int currentOperationProgressTotal;

	/**
	 * 当前处理器当前进度
	 */
	int currentOperationProgressNow;

	/**
	 * 当前处理器是否因为错误而停止
	 */
	boolean isError;

	@JsonGetter
	public int getCurrentProgress()
	{
		if(listOperationName == null || listOperationName.size() == 0 || currentOperationName == null)
			return 0;

		if(Objects.equals(ContextKeys.CONTEXT_FINISHED, currentOperationName))
			return 100;

		final var size = listOperationName.size();
		final float single = 100f / size;
		final var index = listOperationName.indexOf(currentOperationName);
		if(index < 0)
			return 0;
		else
			return (int) (index * single + single * currentOperationProgressNow / currentOperationProgressTotal);
	}
}
