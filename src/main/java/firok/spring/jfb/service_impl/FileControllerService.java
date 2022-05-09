package firok.spring.jfb.service_impl;

import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.jfb.bean.FileInfoBean;
import firok.spring.jfb.config.CacheConfig;
import firok.spring.jfb.config.FFmpegTranscodeConfig;
import firok.spring.jfb.config.MinioConfig;
import firok.spring.jfb.constant.FileTaskStatusEnum;
import firok.spring.jfb.constant.FileTaskTypeEnum;
import firok.spring.jfb.constant.SliceUploadStatusEnum;
import firok.spring.jfb.ioo.rdo.FileTask;
import firok.spring.jfb.ioo.vo.CreateFileTaskVO;
import firok.spring.jfb.ioo.vo.QueryTaskVO;
import firok.spring.jfb.util.NativeProcess;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "SpringJavaAutowiredFieldsWarningInspection", "RedundantSuppression", "UnnecessaryLabelOnBreakStatement"})
@Service
public class FileControllerService
{
	private final Object LOCK = new Object();
	/**
	 * 上传任务列表
	 */
	private final Map<String, FileTask> mapTask = new HashMap<>();

	private static final boolean useMinio = false;
	private static final boolean useQiniu = true;


	@Autowired
	IService<FileInfoBean> serviceFileInfo;

	@Autowired
	CacheConfig configCache;

	@Autowired
	MinioConfig configMinio;

	@Autowired
	FFmpegTranscodeConfig configFFmpeg;

	@Autowired
	MinioClient client;

	@Autowired
	QiniuStorageService serviceQiniu;


	/**
	 * 创建文件任务
	 */
	public CreateFileTaskVO createTask(String fileName, long fileSize, long sliceSize, FileTaskTypeEnum type) throws IOException
	{
		var id = UUID.randomUUID().toString();
		var folder = configCache.createCacheFolder(id);
		var task = new FileTask(id, fileName, fileSize, sliceSize, type, folder);

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
		try(ifs; ofs) {} // 为了捕获到开启流时候的异常 现在得手动关闭流

		synchronized (task.LOCK)
		{
			task.markSliceStatus(sliceIndex, SliceUploadStatusEnum.Uploaded); // 标记为已上传
		}
	}

	public FileTaskStatusEnum getTaskStatus(FileTask task)
	{
		synchronized (task.LOCK)
		{
			return task.getTaskStatus();
		}
	}
	public void setTaskStatus(FileTask task, FileTaskStatusEnum status)
	{
		synchronized (task.LOCK)
		{
			task.setTaskStatus(status);
		}
	}

	/**
	 * 上传切片之后进行的各类操作
	 */
	public void postProcess(FileTask task)
	{
		final var type = task.getType();
		FileTaskStatusEnum status = null;

		// 检查任务状态是否允许开始合并
		final boolean allowMerge;
		status = getTaskStatus(task);
		allowMerge = switch (status) {
			case UploadSuccess, MergeError -> true;
			default -> false;
		};

		if(allowMerge) // 实际的合并代码开始
		{
			try
			{
				var folder = task.getFolder();
				var fileMerge = folder.fileMerge();

				// 检查当前状态是否是前次合并失败
				// 如果为真 则清理前次没有合并完成的文件
				// 但是不准备检查是否所有分片文件都存在 如果检查的话交互流程会麻烦很多
				if(status == FileTaskStatusEnum.MergeError)
				{
					var resultDelete = fileMerge.delete();
					System.out.println("删除前次合并失败文件结果: "+(resultDelete ? "成功" : "失败"));
				}

				setTaskStatus(task, status = FileTaskStatusEnum.MergingSlice);

				try(var ofs = new FileOutputStream(fileMerge))
				{
					// 转移分片
					int countSlice = task.getSliceCount();
					for(int step = 0; step < countSlice; step++)
					{
						var fileSlice = folder.sliceOf(step);
						try(var ifs = new FileInputStream(fileSlice))
						{
							StreamUtils.copy(ifs, ofs);
						}
					}
				}

				setTaskStatus(task, status = FileTaskStatusEnum.MergeSuccess);
			}
			catch (Throwable e)
			{
				setTaskStatus(task, status = FileTaskStatusEnum.MergeError);
				System.err.println("合并分片发生错误");
				e.printStackTrace(System.err);
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

		// 检查任务状态是否允许开始转码切片
		final boolean allowTranscode;
		allowTranscode = type == FileTaskTypeEnum.Video_Slice && switch (status) {
			case MergeSuccess, TranscodeError -> true;
			default -> false;
		};

		PROCESS_TRANSCODE: if(allowTranscode) // 实际的转码代码开始
		{
			// 调用ffmpeg 把合并后的文件作为视频文件转码切片为m3u8
			var folder = task.getFolder();
			var fileMerge = folder.fileMerge();
			var folderTranscode = folder.folderTranscode();
			var fileM3U8 = new File(folderTranscode, task.getId() + ".m3u8");

			var needClean = status == FileTaskStatusEnum.TranscodeError;
			setTaskStatus(task, status = FileTaskStatusEnum.Transcoding);

			// 检查状态 如果需要就先清理前次转码失败的内容
			if(needClean)
			{
				try
				{
					File[] listFileSlice =folder.listFileSlice();
					for(File fileSlice : listFileSlice)
					{
						FileUtils.forceDelete(fileSlice);
					}
				}
				catch (Exception e)
				{
					setTaskStatus(task, status = FileTaskStatusEnum.TranscodeError);
					break PROCESS_TRANSCODE;
				}
			}

			String pathMerge;
			String pathM3U8;
			try
			{
				pathMerge = fileMerge.getCanonicalPath();
				pathM3U8 = fileM3U8.getCanonicalPath();
			}
			catch (IOException e)
			{
				setTaskStatus(task, status = FileTaskStatusEnum.TranscodeError);
				break PROCESS_TRANSCODE;
			}

			var command = """
                    %s -hwaccel auto -i "%s" -hls_time "2" -hls_segment_type "mpegts" -hls_segment_size "500000" -hls_allow_cache "1" -hls_list_size "0" -hls_flags "independent_segments" -c:v copy "%s"
                    """.formatted(configFFmpeg.pathFFmpeg, pathMerge, pathM3U8);

			try(var process = new NativeProcess(command))
			{
				int ret = process.waitFor();
				if(ret != 0)
				{
					var contentErr = process.contentErr();
					throw new RuntimeException("转码发生错误: \n"+contentErr);
				}
				status = FileTaskStatusEnum.TranscodeSuccess;
			}
			catch (Exception e)
			{
				status = FileTaskStatusEnum.TranscodeError;
			}
			finally
			{
				setTaskStatus(task, status);
			}
		}

		// 检查任务状态是否允许开始上传服务器
		final boolean allowUpload;
		allowUpload = switch (type) {
			case Video_Slice -> switch (status) { // 视频任务必须转码成功后才能上传
				case TranscodeSuccess, TransportCancel -> true;
				default -> false;
			};
			case Upload_Single_Big -> switch (status) { // 普通任务在合并完成后上传
				case MergeSuccess, TransportCancel -> true;
				default -> false;
			};
		};

		PROCESS_UPLOAD: if(allowUpload) // 实际的上传代码开始
		{
			var needClean = status == FileTaskStatusEnum.TransportCancel;
			setTaskStatus(task, status = FileTaskStatusEnum.Transporting);

			if(needClean) // 清理MinIO
			{
				// todo 暂时没空写清理代码
			}

			class QueueNode // 笑嘻嘻 你看我想不想把这玩意写成静态的
			{
				/**
				 * 需要上传的文件
				 */
				File file;

				/**
				 * 上传到服务器之后的文件名
				 */
				String uploadFileName;

				/**
				 * 剩余尝试次数 默认尝试3次
				 * 如果出现上传了3次都没成功的文件
				 * 直接停止队列进行并更新任务状态为上传失败
				 */
				int tries;
			}
			// 计算等待上传的文件列表
			var queueFileUpload = new java.util.LinkedList<QueueNode>();
			var folder = task.getFolder();
			var idTask = task.getId();
			switch (type) // 根据任务类型不同 需要上传的文件不同
			{
				case Video_Slice: // 切片转码文件需要把一整个文件夹的内容上传
				{
					var listTranscode = folder.listFileTranscode();
					// fixme 如果出现列表长度为0 就其实已经出错了 暂时没空做处理 后面会对这里做处理

					for(var fileTranscode : listTranscode)
					{
						var node = new QueueNode();
						node.file = fileTranscode;
						node.uploadFileName = fileTranscode.getName(); // todo 计算上传后的文件名
						node.tries = 3;
						queueFileUpload.add(node);
					}
					break;
				}
				case Upload_Single_Big: // 单文件直接上传
				{
					var fileMerge = folder.fileMerge();
					var node = new QueueNode();
					node.file = fileMerge;
					node.uploadFileName = fileMerge.getName(); // todo 计算上传后的文件名
					node.tries = 3;
					queueFileUpload.add(node);
					break;
				}
			}

			// 开始处理上传队列
			QueueNode nodeCurrent; // 当前进行中的上传任务节点
			LOOP_QUEUE: while (true)
			{
				// 获取一个可用的上传任务节点
				if(queueFileUpload.isEmpty()) // 哦吼 都处理完了
				{
					status = FileTaskStatusEnum.TransportSuccess;
					break LOOP_QUEUE;
				}
				else // 还有剩下的
				{
					// 从队列里取第一个出来
					nodeCurrent = queueFileUpload.removeFirst();
					--nodeCurrent.tries;
					if(nodeCurrent.tries < 0) // 啧
					{
						status = FileTaskStatusEnum.TransportSuccess;
						break LOOP_QUEUE;
					}
				}

				if(useMinio)
				{
					// 处理该节点
					try(var ifs = new FileInputStream(nodeCurrent.file))
					{
						// todo high 上传之前应该检查一下 MinIO 的状态
						//           避免前次有没上传完的部分之类的情况
						//           如果有这种情况就先删掉 MinIO 里的数据
						var args = PutObjectArgs.builder()
								.bucket(configMinio.nameBucket)
								.contentType(MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE)
								.stream(ifs, nodeCurrent.file.length(), -1)
								.object(nodeCurrent.uploadFileName)
								.build();
						client.putObject(args);
					}
					catch (Exception e)
					{
						System.err.println("上传文件到 MinIO 时发生错误");
						e.printStackTrace(System.err);
						// 把节点再扔回队列
						queueFileUpload.addLast(nodeCurrent);
					}
				}
				else if(useQiniu)
				{
					// 处理该节点
					try
					{
						serviceQiniu.upload(nodeCurrent.file);
					}
					catch (Exception e)
					{
						System.err.println("上传文件到 七牛云 时发生错误");
						e.printStackTrace(System.err);
						// 把节点再扔回队列
						queueFileUpload.addLast(nodeCurrent);
					}
				}
				else
					throw new RuntimeException("未选中上传服务");

			}
			// 上传队列处理完成 将状态写回任务信息
			setTaskStatus(task, status);

			// 到这里 一次完整的文件处理就结束了
			// todo 判断一下任务的状态 如果是完成/失败的任务就增加一个等待过期标记
			//      完成的任务保存10分钟左右 失败的任务保存1小时左右
			//      如果在规定时间内没有对任务做任何处理 (对于失败的任务可以选择重试)
			//      就从任务列表中移除任务 并开始清理流程 删掉各种缓存文件等
		}
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

		// 全部上传完成, 准备后续操作
		// todo 把这里提升为Thread子类
		var threadPostProcess = new Thread(()-> this.postProcess(task), "task-post-process-" + task.getId());
		threadPostProcess.setDaemon(true);

		synchronized (task.LOCK)
		{
			task.setCurrentThread(threadPostProcess);
			threadPostProcess.start();
		}

		return true;
	}
}


