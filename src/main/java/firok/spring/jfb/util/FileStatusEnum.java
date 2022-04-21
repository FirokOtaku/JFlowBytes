package firok.spring.jfb.util;

/**
 * 缓存文件状态
 */
@SuppressWarnings("unused")
public enum FileStatusEnum
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
	UploadCancel,

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
	 * 向 MinIO 服务器转移数据
	 */
	TransportMinio,

	/**
	 * 转移取消
	 */
	TransportCancel,
}
