package firok.spring.jfb.service.storage;

import firok.spring.jfb.service.ExceptionIntegrative;

import java.io.*;

/**
 * 与存储服务进行交互
 */
public interface IStorageIntegrative
{
	String STORAGE_SERVICE_SUFFIX = "-storage";

	/**
	 * 存储空间名称. 临时方法, 用作寻找储存空间删除文件
	 */
	String getStorageTargetName();

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
		try(var ifs = new FileInputStream(file))
		{
			store(nameBucket, nameObject, ifs);
		}
		catch (Throwable e)
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

	/**
	 * 从持久储存中删除指定内容
	 * @param nameBucket 桶名称
	 * @param namesObject 对象名称
	 * @implNote 调用此方法时 namesObject 参数不为 null 且包含元素, 也就是说方法内部不需要进行相关判断
	 * @see firok.spring.jfb.controller.RecordController#deleteRecord(String)
	 */
	void delete(String nameBucket, String... namesObject) throws ExceptionIntegrative;
}
