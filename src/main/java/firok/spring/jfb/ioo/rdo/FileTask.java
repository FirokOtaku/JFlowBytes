package firok.spring.jfb.ioo.rdo;

import firok.spring.jfb.constant.FileTaskStatusEnum;
import firok.spring.jfb.constant.FileTaskTypeEnum;
import firok.spring.jfb.constant.SliceUploadStatusEnum;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.UUID;

/**
 * 文件操作相关任务
 */
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
	@Getter
	private final String id;

	/**
	 * 原始文件名
	 */
	@Getter
	private final String fileName;

	/**
	 * 文件大小
	 */
	@Getter
	private final long fileSize;

	/**
	 * 切片数量
	 */
	private final int sliceCount;

	public int getSliceCount()
	{
		return type == FileTaskTypeEnum.Upload_Single_Big ? 1 : sliceCount;
	}

	/**
	 * 切片大小
	 */
	@Getter
	private final long sliceSize;

	/**
	 * 任务类型
	 */
	@Getter
	private final FileTaskTypeEnum type;

	@Getter
	private CacheFolder folder;

	/**
	 * 0 - 未上传; 1 - 上传中; 2 - 上传完成; 3 - 上传失败
	 */
	private final SliceUploadStatusEnum[] sliceStatus;

	@Getter @Setter
	private FileTaskStatusEnum taskStatus;

	public FileTask(String fileName, long fileSize, long sliceSize, FileTaskTypeEnum type, CacheFolder folder)
	{
		this(UUID.randomUUID().toString(), fileName, fileSize, sliceSize, type, folder);
	}
	public FileTask(String id, String fileName, long fileSize, long sliceSize, FileTaskTypeEnum type, CacheFolder folder)
	{
		this.id = id;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.sliceCount = (int) (fileSize / sliceSize + (fileSize % sliceSize == 0 ? 0 : 1));
		this.sliceSize = sliceSize;
		this.type = type;

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

	/**
	 * 当前正在进行中的进程
	 */
	private Thread thread;
	public Thread getCurrentThread()
	{
		return thread;
	}
	public void setCurrentThread(Thread thread)
	{
		this.thread = thread;
	}
}
