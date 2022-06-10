package firok.spring.jfb.hash;

/**
 * 计算映射后的哈希值
 */
public interface IMappedHash
{
	/**
	 * 获取映射器
	 */
	IHashMapper<? extends IMappedHash> getMapper();

	/**
	 * 获取映射后的哈希字符串
	 */
	String getHashString();
}
