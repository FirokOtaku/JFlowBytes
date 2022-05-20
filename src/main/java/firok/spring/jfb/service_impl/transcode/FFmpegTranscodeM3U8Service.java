package firok.spring.jfb.service_impl.transcode;

import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.transcode.ITranscodeM3U8Integrative;
import firok.spring.jfb.service_impl.ContextKeys;
import firok.spring.jfb.util.NativeProcess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static firok.spring.jfb.service_impl.ContextKeys.KEY_FILES;

/**
 * 调用 FFmpeg 把单视频文件转码成 M3U8 视频文件列表
 * @implNote 启用这个 service 需要保证配置文件同时启用 ffmpeg 和转码服务
 */
//@ConditionalOnExpression("${app.service-transcode.ffmpeg-m3u8.enable}")
@Service
public class FFmpegTranscodeM3U8Service extends FFmpegTranscodeService implements ITranscodeM3U8Integrative, IWorkflowService
{
	public static final String SERVICE_NAME = ContextKeys.PREFIX + "ffmpeg-transcode-m3u8";

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

		var command = """
                    %s -hwaccel auto -i "%s" -hls_time "2" -hls_segment_type "mpegts" -hls_segment_size "500000" -hls_allow_cache "1" -hls_list_size "0" -hls_flags "independent_segments" -c:v copy "%s"
                    """.formatted(super.pathFFmpeg, pathVideo, pathM3U8);

		try(var process = new NativeProcess(command))
		{
			int ret = process.waitFor();
			if(ret != 0)
			{
				var contentErr = process.contentErr();
				throw new RuntimeException("转码发生错误: \n"+contentErr);
			}
		}
		catch (Exception e)
		{
			throw new ExceptionIntegrative("转码发生错误", e);
		}
	}

	@Override
	public void operateWorkflow(WorkflowContext context) throws ExceptionIntegrative
	{
		var fileMerge = context.get(KEY_FILES) instanceof File[] files && files.length == 1? files[0] : null;
		if(fileMerge == null)
			throw new ExceptionIntegrative("没有找到合并后的视频文件");
		var folderM3U8 = folderM3U8of(context);
		var fileM3U8 = fileM3U8of(folderM3U8, context.id);

		try
		{
			// 合理化路径
			var pathFileMerge = fileMerge.getCanonicalPath();
			var pathFileM3U8 = fileM3U8.getCanonicalPath();

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
					throw new RuntimeException("转码发生错误: \n"+contentErr);
				}
			}
			// 如果成功转码 删掉转码前的文件
			IWorkflowService.super.addFileToCleanList(context, fileMerge);

			// 更新上下文
			var files = fileM3U8.getParentFile().listFiles();
			if(files == null) files = new File[0];
			context.put(KEY_FILES, files);
			context.put(KEY_FOLDER_M3U8, folderM3U8);
			context.put(KEY_FILE_M3U8, fileM3U8);
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
			throw new ExceptionIntegrative(e);
		}
	}
}
