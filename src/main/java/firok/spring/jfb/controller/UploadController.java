package firok.spring.jfb.controller;

import firok.spring.jfb.bean.Ret;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 上传相关接口
 *
 * 一般文件, 由前端切小片上传, 由后端组装并交由 MinIO 切片储存.
 * 视频文件在前端切片上传完成之后, 调用 ffmpeg 切片, 切片完成之后把所有文件数据放进 MinIO, 然后删除缓冲文件.
 */
@RestController
public class UploadController
{
	/**
	 * 上传任务列表
	 */
	private List<?> listTask;

	/**
	 * 创建上传任务, 这会返回一个任务信息, 客户端需要根据任务信息把所有分片上传至服务器
	 */
	public Ret<?> createTaskUploadCache()
	{
		return Ret.success();
	}

	/**
	 * 向某个任务上传一个文件分片
	 */
	public Ret<?> uploadSlice()
	{
		return Ret.success();
	}

	/**
	 * 上传文件任务实体
	 */
	class UploadTask
	{
		/**
		 * 原始文件名
		 */
		String fileName;

		/**
		 * 文件大小
		 */
		long fileSize;

		/**
		 * 切片数量
		 */
		int sliceCount;

		/**
		 * 切片大小
		 */
		int sliceSize;
	}
}
