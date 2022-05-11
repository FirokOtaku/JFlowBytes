package firok.spring.jfb.constant;

/**
 * 文件任务类型
 */
@Deprecated
public enum FileTaskTypeEnum
{
	/**
	 * 普通文件 - 纯上传
	 */
	Upload_Single_Big,

	/**
	 * 视频 - 转码切片
	 */
	Video_Slice,
}
