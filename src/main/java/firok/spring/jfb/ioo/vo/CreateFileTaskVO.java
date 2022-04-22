package firok.spring.jfb.ioo.vo;

import firok.spring.jfb.ioo.rdo.FileTask;

/**
 * 返回值 - 创建文件任务
 */
public record CreateFileTaskVO(String id)
{
	public static CreateFileTaskVO fromTask(FileTask task)
	{
		return new CreateFileTaskVO(task.getId());
	}
}
