package firok.spring.jfb.service.storage;

import firok.spring.jfb.service.ExceptionIntegrative;

import java.io.*;

/**
 * 与存储服务进行交互
 *
 * todo 后面需要扩展功能的时候再继续抽象
 */
public interface IStorageIntegrative
{
	/**
	 * 将指定数据持久化存储
	 * @param nameBucket 储存空间名称
	 * @param nameObject 对象名称
	 * @param is 对象数据输入流
	 */
	void store(String nameBucket, String nameObject, InputStream is) throws ExceptionIntegrative;

	default void storeByFile(String nameBucket, File file) throws ExceptionIntegrative
	{
		String nameObject = file.getName();
		try
		{
			store(nameBucket, nameObject, new FileInputStream(file));
		}
		catch (FileNotFoundException e)
		{
			throw new ExceptionIntegrative(e);
		}
	}

	/**
	 * 从持久储存中提取指定数据, 输出到指定流内
	 * @param nameBucket 储存空间名称
	 * @param nameObject 对象名称
	 * @param os 目标输出流
	 */
	void extract(String nameBucket, String nameObject, OutputStream os) throws ExceptionIntegrative;
}
