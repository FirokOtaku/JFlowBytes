package firok.spring.jfb.flow;

import firok.spring.jfb.constant.ContextKeys;
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

			try
			{
				// 检查上下文是否正确
				var isContextSuitable = service.isWorkflowContextSuitable(context);
				if(!isContextSuitable)
				{
//					context.log(Level.SEVERE, "工作流上下文不满足需求, 强制停止工作流");
					throw new ExceptionIntegrative("工作流上下文不满足需求, 强制停止工作流");
				}

				var nameService = service.getWorkflowServiceOperation();
				context.log(Level.INFO, "操作工作流开始: " + nameService);
				service.operateWorkflow(context);
				context.log(Level.INFO, "操作工作流完成: " + nameService);
			}
			catch (ExceptionIntegrative e)
			{
				context.log(Level.SEVERE, "操作工作流发生异常: " + e.getMessage());
				context.exception = e;
				e.printStackTrace(System.err);
			}
			catch (Exception e)
			{
				context.log(Level.SEVERE, "操作工作流发生未知异常: " + e.getMessage());
				context.exception = e;
				e.printStackTrace(System.err);
			}
			finally
			{
				try
				{
					context.log(Level.INFO, "清理工作流开始");
					service.cleanWorkflow(context, context.exception == null);
					context.log(Level.INFO, "清理工作流完成");
				}
				catch (ExceptionIntegrative e)
				{
					context.log(Level.SEVERE, "清理工作流发生异常: " + e.getMessage());
					context.exception = e;
				}

				// 如果工作流执行过程出现任何错误 则停止工作流
				if(context.exception != null)
					break TICK_LOOP;
			}
		}

		// 工作流完成之后 保留5分钟
		// 如果工作流自己加了这个限制变量就不用在这里加了
		if(!context.containsKey(ContextKeys.KEY_TIME_TIMEOUT_LIMIT))
			context.put(ContextKeys.KEY_TIME_TIMEOUT_LIMIT, System.currentTimeMillis() + 300_000);

		try
		{
			WorkflowServices.cleanWorkflow(context, false, true);
		}
		catch (Exception ignored) { }
		context.currentOperationIndex = Integer.MAX_VALUE;
		context.log(Level.INFO, "工作流结束");
	}
}
