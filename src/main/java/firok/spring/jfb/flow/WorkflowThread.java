package firok.spring.jfb.flow;

import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;

import java.util.logging.Level;

/**
 * 工作流操作线程
 */
public class WorkflowThread extends Thread
{
	final WorkflowContext context;
	public WorkflowThread(WorkflowContext context)
	{
		if(context == null)
			throw new IllegalArgumentException("工作流上下文为空");

		this.context = context;
	}

	@SuppressWarnings({"UnnecessaryLabelOnBreakStatement", "ConstantConditions"})
	@Override
	public void run()
	{
		context.currentOperationIndex = Integer.MIN_VALUE;
		context.log(Level.INFO, "工作流开始");
		IWorkflowService service;
		TICK_LOOP: while(context.hasNextOperation())
		{
			service = context.nextOperation();
			// 检查上下文是否正确
			var isContextSuitable = service.isWorkflowContextSuitable(context);
			if(!isContextSuitable)
			{
				context.log(Level.SEVERE, "工作流上下文不满足需求, 强制停止工作流");
				break TICK_LOOP;
			}

			boolean isOperationSuccess = false;
			try
			{
				var nameService = service.getWorkflowServiceOperation();
				context.log(Level.INFO, "操作工作流开始: " + nameService);
				service.operateWorkflow(context);
				context.log(Level.INFO, "操作工作流完成: " + nameService);
				isOperationSuccess = true;
			}
			catch (ExceptionIntegrative e)
			{
				context.log(Level.SEVERE, "操作工作流发生异常: " + e.getMessage());
				e.printStackTrace(System.err);
				isOperationSuccess = false;
			}
			catch (Exception e)
			{
				context.log(Level.SEVERE, "操作工作流发生未知异常: " + e.getMessage());
				e.printStackTrace(System.err);
				isOperationSuccess = false;
			}

			if(!isOperationSuccess)
				break TICK_LOOP;
		}
		context.currentOperationIndex = Integer.MAX_VALUE;
		context.log(Level.INFO, "工作流结束");
	}
}
