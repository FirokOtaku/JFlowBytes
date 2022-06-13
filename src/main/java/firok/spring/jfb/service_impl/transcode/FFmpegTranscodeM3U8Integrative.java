package firok.spring.jfb.service_impl.transcode;

import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.transcode.ITranscodeM3U8Integrative;
import firok.spring.jfb.constant.ContextKeys;
import firok.topaz.NativeProcess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static firok.spring.jfb.constant.ContextKeys.KEY_FILES;

/**
 * 调用 FFmpeg 把单视频文件转码成 M3U8 视频文件列表
 * @implNote 启用这个 service 需要保证配置文件同时启用 ffmpeg 和转码服务
 */
@ConditionalOnExpression("${app.service-transcode.ffmpeg-base.enable} && ${app.service-transcode.ffmpeg-m3u8.enable}")
@Service
public class FFmpegTranscodeM3U8Integrative extends FFmpegTranscodeIntegrative implements ITranscodeM3U8Integrative, IWorkflowService
{
	public static final String SERVICE_NAME = ContextKeys.PREFIX + "ffmpeg-transcode-m3u8";

	public static final String KEY_THUMBNAIL_WIDTH = "thumbnail-width";
	public static final String KEY_THUMBNAIL_HEIGHT = "thumbnail-height";

	@Value("${app.service-transcode.ffmpeg-m3u8.folder-transcode-m3u8}")
	protected String folderM3U8;

	/**
	 * 获取一个m3u8目录
	 */
	protected File folderM3U8of(WorkflowContext context)
	{
		return new File(context.folderWorkflowRoot, folderM3U8);
	}

	/**
	 * 获取一个m3u8文件
	 */
	protected File fileM3U8of(File folderM3U8, String name)
	{
		return new File(folderM3U8, name + ".m3u8");
	}

	/**
	 * 获取一个视频缩略图文件
	 */
	protected File fileThumbnailOf(WorkflowContext context)
	{
		return new File(context.folderWorkflowRoot, context.id + ".jpg");
	}

	@Override
	public String getWorkflowServiceOperation()
	{
		return SERVICE_NAME;
	}

	@Override
	public Map<String, Class<?>> getWorkflowParamContext()
	{
		var ret = IWorkflowService.super.getWorkflowParamContext();
		ret.put(KEY_FILES, File[].class);
		return ret;
	}

	public static final String KEY_FOLDER_M3U8 = "folder_m3u8";
	public static final String KEY_FILE_M3U8 = "file_m3u8";

	@Override
	public void toM3U8(File fileVideo, File fileM3U8) throws ExceptionIntegrative
	{
		// 调用ffmpeg 把合并后的文件作为视频文件转码切片为m3u8

		String pathVideo;
		String pathM3U8;
		try
		{
			pathVideo = fileVideo.getCanonicalPath();
			pathM3U8 = fileM3U8.getCanonicalPath();
		}
		catch (IOException e)
		{
			throw new ExceptionIntegrative("合理化路径时发生错误", e);
		}

		// 检查视频元信息
		// 给前台提供更好的错误返回信息
		var commandCheck = """
				%s -find_stream_info "%s"
				""".formatted(super.pathFFprobe, pathVideo);

		try(var process = new NativeProcess(commandCheck))
		{
			int ret = process.waitFor();
			if(ret != 0)
			{
				var contentErr = process.contentErr();
				throw new RuntimeException(contentErr);
			}
		}
		catch (Exception e)
		{
			throw new ExceptionIntegrative("检测视频文件元信息时发生错误, 视频文件格式不受支持或已损坏, 请检查视频文件: " + e.getMessage(), e);
		}

		// 处理视频 转码切片
		var commandProcess = """
                    %s -hwaccel auto -i "%s" -hls_time "2" -hls_segment_type "mpegts" -hls_segment_size "500000" -hls_allow_cache "1" -hls_list_size "0" -hls_flags "independent_segments" -c:v copy "%s"
                    """.formatted(super.pathFFmpeg, pathVideo, pathM3U8);

		try(var process = new NativeProcess(commandProcess))
		{
			int ret = process.waitFor();
			if(ret != 0)
			{
				var contentErr = process.contentErr();
				throw new RuntimeException(contentErr);
			}
		}
		catch (Exception e)
		{
			throw new ExceptionIntegrative("转码发生错误: " + e.getMessage(), e);
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Override
	public void operateWorkflow(WorkflowContext context) throws ExceptionIntegrative
	{
		var fileMerge = context.get(KEY_FILES) instanceof File[] files && files.length == 1? files[0] : null;
		if(fileMerge == null)
			throw new ExceptionIntegrative("没有找到合并后的视频文件");
		var folderM3U8 = folderM3U8of(context);
		var fileM3U8 = fileM3U8of(folderM3U8, context.id);
		var fileThumbnail = fileThumbnailOf(context);

		String pathFileMerge, pathFileM3U8, pathFileThumbnail;
		try // 合理化路径
		{
			pathFileMerge = fileMerge.getCanonicalPath();
			pathFileM3U8 = fileM3U8.getCanonicalPath();
			pathFileThumbnail = fileThumbnail.getCanonicalPath();
		}
		catch (Exception e)
		{
			throw new ExceptionIntegrative("无法合理化路径", e);
		}

		try // 生成缩略图
		{
			var command = """
					%s -y -i "%s" -r 1 -frames:v 1 -f image2 "%s"
					""".formatted(super.pathFFmpeg, pathFileMerge, pathFileThumbnail);
			try(var process = new NativeProcess(command))
			{
				int ret = process.waitFor();
				if(ret != 0)
				{
					var contentErr = process.contentErr();
					throw new RuntimeException(contentErr);
				}
			}

			// todo 压缩缩略图大小
		}
		catch (Exception e)
		{
			throw new ExceptionIntegrative("生成缩略图发生错误:\n" + e.getMessage());
		}

		try
		{

			// 创建目录 准备开始转码
			folderM3U8.mkdirs();

			// 用指令创建本地线程对文件进行转码
			var command = """
                    %s -hwaccel auto -i "%s" -hls_time "2" -hls_segment_type "mpegts" -hls_segment_size "500000" -hls_allow_cache "1" -hls_list_size "0" -hls_flags "independent_segments" -c:v copy "%s"
                    """.formatted(super.pathFFmpeg, pathFileMerge, pathFileM3U8);
			try(var process = new NativeProcess(command))
			{
				int ret = process.waitFor();
				if(ret != 0)
				{
					var contentErr = process.contentErr();
					throw new RuntimeException(contentErr);
				}
			}
		}
		catch (InterruptedException e)
		{
			// 如果转码失败 删掉转码后目录所有内容
			IWorkflowService.super.addFileToCleanList(context, folderM3U8);
			throw new ExceptionIntegrative("用户取消工作流或转码线程中断", e);
		}
		catch (Exception e)
		{
			// 如果转码失败 删掉转码后目录所有内容
			IWorkflowService.super.addFileToCleanList(context, folderM3U8);
			throw new ExceptionIntegrative("转码发生错误:\n" + e.getMessage(), e);
		}

		synchronized (context.LOCK)
		{
			// 如果成功转码 删掉转码前的文件
			IWorkflowService.super.addFileToCleanList(context, fileMerge);

			// 转码后的文件列表
			var files = fileM3U8.getParentFile().listFiles();
			if(files == null) files = new File[0];

			// 把缩略图文件追加到列表里
			var filesNew = new File[files.length + 1];
			System.arraycopy(files, 0, filesNew, 0, files.length);
			filesNew[files.length] = fileThumbnail;

			// 更新上下文
			context.put(KEY_FILES, filesNew);
			context.put(KEY_FOLDER_M3U8, folderM3U8);
			context.put(KEY_FILE_M3U8, fileM3U8);
		}
	}
}
