package firok.spring.jfb.ioo.rdo;

import firok.spring.jfb.constant.FileTaskStatusEnum;
import firok.spring.jfb.constant.SliceUploadStatusEnum;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.util.Arrays;
import java.util.UUID;

/**
 * 文件操作相关任务
 */
@Data
public class FileTask
{
	/**
	 * 多线程数据锁
	 */
	@Getter(AccessLevel.NONE)
	public final Object LOCK = new Object();

	/**
	 * 唯一任务id, 一般是一个uuid
	 */
	private final String id;

	/**
	 * 原始文件名
	 */
	private final String fileName;

	/**
	 * 文件大小
	 */
	private final long fileSize;

	/**
	 * 切片数量
	 */
	private final int sliceCount;

	/**
	 * 切片大小
	 */
	private final long sliceSize;

	private CacheFolder folder;

	/**
	 * 0 - 未上传; 1 - 上传中; 2 - 上传完成; 3 - 上传失败
	 */
	private final SliceUploadStatusEnum[] sliceStatus;

	private FileTaskStatusEnum taskStatus;

	public FileTask(String fileName, long fileSize, int sliceCount, long sliceSize, CacheFolder folder)
	{
		this(UUID.randomUUID().toString(), fileName, fileSize, sliceCount, sliceSize, folder);
	}
	public FileTask(String id, String fileName, long fileSize, int sliceCount, long sliceSize, CacheFolder folder)
	{
		this.id = id;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.sliceCount = sliceCount;
		this.sliceSize = sliceSize;

		this.sliceStatus = new SliceUploadStatusEnum[sliceCount];
		Arrays.fill(sliceStatus, SliceUploadStatusEnum.NotUploaded); // 等待切片上传

		this.taskStatus = FileTaskStatusEnum.NotStarted;
		this.folder = folder;
	}

	/**
	 * 标记此任务状态
	 */
	public void markTaskStatus(FileTaskStatusEnum status)
	{
		this.taskStatus = status;
	}

	/**
	 * 标记某切片上传状态
	 */
	public void markSliceStatus(int sliceIndex, SliceUploadStatusEnum status)
	{
		this.sliceStatus[sliceIndex] = status;
	}

	public SliceUploadStatusEnum getSliceStatus(int sliceIndex)
	{
		return this.sliceStatus[sliceIndex];
	}

	/**
	 * 所有切片是否完成上传
	 */
	public boolean hasAllUploaded()
	{
		for (var status : this.sliceStatus)
			if(status != SliceUploadStatusEnum.Uploaded) return false;
		return true;
	}
}
