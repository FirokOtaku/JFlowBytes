package firok.spring.jfb.constant;

/**
 * 缓存文件状态
 *
 * 详见 doc/task_file_upload.drawio
 */
@SuppressWarnings("unused")
@Deprecated
public enum FileTaskStatusEnum
{
	/**
	 * 未开始上传
	 */
	NotStarted,

	/**
	 * 上传切片中
	 */
	UploadingSlice,

	/**
	 * 上传取消
	 */
	UploadError,

	/**
	 * 上传完成
	 */
	UploadSuccess,

	/**
	 * 合并切片中
	 */
	MergingSlice,

	/**
	 * 合并取消
	 */
	MergeError,

	/**
	 * 合并完成
	 */
	MergeSuccess,

	/**
	 * 转码切片中
	 */
	Transcoding,

	/**
	 * 转码切片取消
	 */
	TranscodeError,

	/**
	 * 转码切片完成
	 */
	TranscodeSuccess,

	/**
	 * 向 MinIO 服务器转移数据
	 */
	Transporting,

	/**
	 * 转移取消
	 */
	TransportCancel,

	/**
	 * 转移成功
	 */
	TransportSuccess,


	/**
	 * 任务已经结束
	 */
	Finished,
	;
}
