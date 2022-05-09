package firok.spring.jfb.service;

/**
 * 各步骤可能抛出的异常
 */
public class ExceptionIntegrative extends Exception
{
	public ExceptionIntegrative(String message, Exception inner)
	{
		super(message, inner);
	}
	public ExceptionIntegrative(Exception inner)
	{
		super(inner);
	}
	public ExceptionIntegrative(String message)
	{
		super(message);
	}

	// 不一定用得上的玩意
	public Throwable getRealException()
	{
		var exception = this.getCause();
		if(exception instanceof ExceptionIntegrative e)
		{
			return e.getRealException();
		}
		else // exception == null || exception instanceof Throwable
		{
			return exception;
		}
	}
}
