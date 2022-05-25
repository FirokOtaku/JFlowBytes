package firok.spring.jfb.service.merge;

import firok.spring.jfb.service.ExceptionIntegrative;

import java.io.File;

/**
 * 合并文件
 */
public interface IFileMergeIntegrative
{
	/**
	 * @param files 文件列表
	 * @param fileMerge 目标合并文件
	 */
	void mergeAll(File[] files, File fileMerge) throws ExceptionIntegrative;
}
