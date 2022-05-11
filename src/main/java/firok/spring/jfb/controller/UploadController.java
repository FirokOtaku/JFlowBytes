package firok.spring.jfb.controller;

import firok.spring.jfb.bean.Ret;
import firok.spring.jfb.ioo.ro.CreateTaskParam;
import firok.spring.jfb.ioo.vo.CreateFileTaskVO;
import firok.spring.jfb.ioo.vo.QueryTaskVO;
import firok.spring.jfb.service_impl.FileControllerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.Callable;

/**
 * 上传相关接口
 *
 * 一般文件, 由前端切小片上传, 由后端组装并交由 MinIO 切片储存.
 * 视频文件在前端切片上传完成之后, 调用 ffmpeg 切片, 切片完成之后把所有文件数据放进 MinIO, 然后删除缓冲文件.
 */
@RestController
@RequestMapping("/api/upload")
@Deprecated
public class UploadController
{
	@Autowired
	FileControllerService service;

	/**
	 * 创建上传任务, 这会返回一个任务信息, 客户端需要根据任务信息把所有分片上传至服务器
	 */
	@PostMapping("/createTask")
	public Ret<CreateFileTaskVO> createTask(
			@RequestBody CreateTaskParam param
	) {
		try
		{
			var task = service.createTask(
					param.fileName(),
					param.fileSize(),
					param.sliceSize(),
					param.type()
			);
			return Ret.success(task);
		}
		catch (Exception e)
		{
			return Ret.fail(e.getMessage());
		}
	}

	@GetMapping("/queryTask")
	public Ret<QueryTaskVO> queryTask(
			@RequestParam("idTask") String idTask
	) {
		try
		{
			var task = service.getTask(idTask);
			if(task == null) throw new IllegalArgumentException("任务不存在或已超时");
			return Ret.success(task);
		}
		catch (Exception e)
		{
			return Ret.fail(e.getMessage());
		}
	}

	/**
	 * 向某个任务上传一个文件分片
	 */
	@PostMapping("/uploadSlice")
	public WebAsyncTask<Ret<?>> uploadSlice(
			@RequestPart("file") MultipartFile file, // 文件流
			@RequestParam("taskId") String idTask, // 任务id
			@RequestParam("sliceIndex") int indexSlice // 分片号
	) // fixme 后面可能需要配置WebAsyncTask的线程池
	{
		Callable<Ret<?>> callable = () -> {
			try
			{
				service.uploadSlice(idTask, indexSlice, file);
				return Ret.success();
			}
			catch (Exception e)
			{
				return Ret.fail(e);
			}
		};

		var ret = new WebAsyncTask<>(callable);

		ret.onCompletion(() -> {
			// 检查任务状态
			// 如果上传完成 就合并操作
			service.checkTaskUploadFinish(idTask);
		});
		return ret;
	}

}

