package firok.spring.jfb.hash;

import java.util.Objects;

/**
 * 用于根据提取某个字符串中的哈希值
 */
public interface IHashMapper<TypeHash extends IMappedHash>
{
	/**
	 * 获取映射器名称
	 */
	String getMapperName();

	/**
	 * 将某个字符串映射为哈希值
	 */
	TypeHash mapHash(String str);

	/**
	 * 将某个哈希值映射为字符串
	 * */
	String mapOrigin(TypeHash hash);

	static IHashMapper<?> getMapper(String name)
	{
		var loader = java.util.ServiceLoader.load(IHashMapper.class);
		for (IHashMapper<?> mapper : loader)
		{
			if(Objects.equals(mapper.getMapperName(), name))
				return mapper;
		}
		return null;
	}
}
