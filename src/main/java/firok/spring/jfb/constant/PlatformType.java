package firok.spring.jfb.constant;

/**
 * 操作系统类型
 */
public abstract class PlatformType
{
	String OS_NAME = System.getProperty("os.name").toLowerCase();

	protected PlatformType()
	{
		this.isCurrent = equalsCurrent();
	}

	private final boolean isCurrent;
	/**
	 * 判断是否是当前系统
	 */
	public final boolean isCurrent()
	{
		return isCurrent;
	}

	/**
	 * 子类需要实现此方法 返回当前是否是此操作系统
	 */
	protected abstract boolean equalsCurrent();

	/**
	 * 未知操作系统
	 */
	public static final PlatformType Unknown = new PlatformType()
	{
		@Override
		protected boolean equalsCurrent()
		{
			return false;
		}
	};

	/**
	 * Windows 操作系统
	 */
	public static final PlatformType Windows = new PlatformType()
	{
		@Override
		protected boolean equalsCurrent()
		{
			return OS_NAME.contains("windows");
		}
	};

	public static final PlatformType Linux = new PlatformType()
	{
		@Override
		protected boolean equalsCurrent()
		{
			// fixme low 有心情再说
			return false;
		}
	};

	public static final PlatformType MacOS = new PlatformType()
	{
		@Override
		protected boolean equalsCurrent()
		{
			// gossip 没机会 也没心情
			return false;
		}
	};



}
