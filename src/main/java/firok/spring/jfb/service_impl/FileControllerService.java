package firok.spring.jfb.service_impl;

import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.jfb.bean.FileInfoBean;
import firok.spring.jfb.config.CacheConfig;
import firok.spring.jfb.constant.SliceUploadStatusEnum;
import firok.spring.jfb.ioo.rdo.FileTask;
import firok.spring.jfb.ioo.vo.CreateFileTaskVO;
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
	public CreateFileTaskVO createTask(String fileName, long fileSize, int sliceCount, long sliceSize) throws IOException
	{
		var id = UUID.randomUUID().toString();
		var folder = configCache.createCacheFolder(id);
		var task = new FileTask(id, fileName, fileSize, sliceCount, sliceSize, folder);

		synchronized (LOCK)
		{
			mapTask.put(id, task);
		}

		return CreateFileTaskVO.fromTask(task);
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
	 * 检查任务, 如果条件合适就进行下一步
	 */
	public void checkTask(String idTask)
	{
		FileTask task;
		synchronized (LOCK)
		{
			task = mapTask.get(idTask);
		}
		if(task == null || !task.hasAllUploaded()) return; // 任务不存在或未完成



	}
}
