package firok.spring.jfb.service_impl;

import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.jfb.bean.FileInfoBean;
import firok.spring.jfb.config.CacheConfig;
import firok.spring.jfb.constant.FileTaskStatusEnum;
import firok.spring.jfb.constant.SliceUploadStatusEnum;
import firok.spring.jfb.ioo.rdo.FileTask;
import firok.spring.jfb.ioo.vo.CreateFileTaskVO;
import firok.spring.jfb.ioo.vo.QueryTaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "SpringJavaAutowiredFieldsWarningInspection", "RedundantSuppression"})
@Service
public class FileControllerService
{
	private final Object LOCK = new Object();
	/**
	 * 上传任务列表
	 */
	private final Map<String, FileTask> mapTask = new HashMap<>();



	@Autowired
	IService<FileInfoBean> serviceFileInfo;

	@Autowired
	CacheConfig configCache;


	/**
	 * 创建文件任务
	 */
	public CreateFileTaskVO createTask(String fileName, long fileSize, long sliceSize) throws IOException
	{
		var id = UUID.randomUUID().toString();
		var folder = configCache.createCacheFolder(id);
		var task = new FileTask(id, fileName, fileSize, sliceSize, folder);

		synchronized (LOCK)
		{
			mapTask.put(id, task);
		}

		return CreateFileTaskVO.fromTask(task);
	}

	/**
	 * 获取任务状态
	 */
	public QueryTaskVO getTask(String idTask)
	{
		synchronized (LOCK)
		{
			var task = mapTask.get(idTask);
			if(task == null) return null;
			synchronized (task.LOCK)
			{
				return new QueryTaskVO(task.getId(), String.valueOf(task.getTaskStatus()));
			}
		}
	}

	/**
	 * @param idTask 任务id
	 * @param sliceIndex 分片号
	 * @param fileSource 上传的分片文件
	 */
	public void uploadSlice(String idTask, int sliceIndex, MultipartFile fileSource) throws IllegalArgumentException, IOException
	{
		FileTask task;
		synchronized (LOCK)
		{
			task = mapTask.get(idTask);
		}
		if(task == null) throw new IllegalArgumentException("任务不存在或已过期");

		if(sliceIndex >= task.getSliceCount())
			throw new IllegalArgumentException("分片号超出范围");

		synchronized (task.LOCK)
		{
			var sliceStatus = task.getSliceStatus(sliceIndex);
			if(sliceStatus != SliceUploadStatusEnum.NotUploaded)
				throw new IllegalArgumentException("分片正在上传或已上传");
			task.markSliceStatus(sliceIndex, SliceUploadStatusEnum.Uploading); // 标记为正在上传
		}

		// 把上传的分片文件保存到缓存区
		InputStream ifs;
		OutputStream ofs;
		try { ifs = fileSource.getInputStream(); } catch (IOException e) { throw new IOException("获取上传分片文件失败", e); }
		var of = task.getFolder().sliceOf(sliceIndex);
		try { ofs = new FileOutputStream(of); } catch (IOException e) { throw new IOException("创建分片缓存文件失败", e); }
		try { StreamUtils.copy(ifs, ofs); } catch (IOException e) { throw new IOException("复制分片缓存数据失败", e); }

		synchronized (task.LOCK)
		{
			task.markSliceStatus(sliceIndex, SliceUploadStatusEnum.Uploaded); // 标记为已上传
		}
	}

	/**
	 * 合并切片
	 */
	public void mergeSlice(FileTask task)
	{
		synchronized (task.LOCK)
		{
			if(task.getTaskStatus() != FileTaskStatusEnum.UploadSuccess)
				return;

			task.setTaskStatus(FileTaskStatusEnum.MergingSlice);
		}

		FileTaskStatusEnum status = FileTaskStatusEnum.MergeError;
		try
		{
			// 实际的合并代码开始
			var folder = task.getFolder();
			var fileMerge = folder.fileMerge();

			try(var ofs = new FileOutputStream(fileMerge))
			{
				// 转移分片
				int countSlice = task.getSliceCount();
				for(int step = 0; step < countSlice; step++)
				{
					var fileSlice = folder.sliceOf(step);
					var ifs = new FileInputStream(fileSlice);

					StreamUtils.copy(ifs, ofs);
				}
			}

			// 更新任务状态
			status = FileTaskStatusEnum.MergeSuccess;
		}
		catch (Throwable e)
		{
			System.err.println("合并分片发生错误");
			e.printStackTrace(System.err);
			status = FileTaskStatusEnum.MergeError;
		}
		finally
		{
			synchronized (task.LOCK)
			{
				task.setCurrentThread(null);
				task.setTaskStatus(status);
			}
		}
	}

	/**
	 * 转码切片合并后的文件
	 */
	public void transformMerge(FileTask task)
	{
		return;
	}

	/**
	 * 检查任务是否上传完成, 如果条件合适就进行下一步
	 * @return 上传是否完成
	 */
	public boolean checkTaskUploadFinish(String idTask)
	{
		FileTask task;
		synchronized (LOCK)
		{
			task = mapTask.get(idTask);
		}
		if(task == null) return false; // 任务不存在或未完成
		synchronized (task.LOCK)
		{
			var status = task.hasAllUploaded() ? FileTaskStatusEnum.UploadSuccess : FileTaskStatusEnum.UploadingSlice;
			task.markTaskStatus(status); // 刷新上传状态
			if(status != FileTaskStatusEnum.UploadSuccess) // 还没上传完
				return false; // 等待继续上传
		}

		// 全部上传完成, 准备合并
		var threadMerge = new Thread(()-> this.mergeSlice(task), "merge-slice-" + task.getId());
		threadMerge.setDaemon(true);

		synchronized (task.LOCK)
		{
			task.setCurrentThread(threadMerge);
			threadMerge.start();
		}

		return true;
	}

	/**
	 * 检查分片是否合并完成, 如果条件合适就进行下一步
	 * @return 合并是否完成
	 * */
	public boolean checkTaskMergeFinish(String idTask)
	{
		FileTask task;
		synchronized (LOCK)
		{
			task = mapTask.get(idTask);
		}
		if(task == null) return false;
		synchronized (task.LOCK)
		{
			var status = task.getTaskStatus();
			if(status != FileTaskStatusEnum.MergeSuccess)
				return false; // 出现错误或当前阶段不正确 不能开始下一步
		}

		// 合并完成
		// 检查是否需要进行视频切片
		// 如果不需要就开始往 MinIO 里面倒腾

		return true;
	}
}
