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
			context.nextOperation();
			if(context.currentOperationIndex == context.listOperation.size())
			{
				context.log(Level.INFO, "工作流完成");
				break TICK_LOOP;
			}

			// fixme low 这个地方其实有问题
			service = context.getCurrentOperation();
			if(service == null)
			{
				context.log(Level.SEVERE, "无法确定下一步工作流操作");
				return;
			}

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
			finally
			{
				try
				{
					context.log(Level.INFO, "清理工作流开始");
					service.cleanWorkflow(context, isOperationSuccess);
					context.log(Level.INFO, "清理工作流完成");
				}
				catch (ExceptionIntegrative e)
				{
					context.log(Level.SEVERE, "清理工作流发生异常: " + e.getMessage());
					break TICK_LOOP;
				}

				if(!isOperationSuccess)
					break TICK_LOOP;
			}
		}
		try
		{
			WorkflowServices.cleanWorkflow(context, false, true);
		}
		catch (Exception ignored) { }
		context.currentOperationIndex = Integer.MAX_VALUE;
		context.log(Level.INFO, "工作流结束");
	}
}
