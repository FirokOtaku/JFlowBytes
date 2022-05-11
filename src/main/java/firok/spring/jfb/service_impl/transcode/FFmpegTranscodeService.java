package firok.spring.jfb.service_impl.transcode;

import firok.spring.jfb.constant.PlatformType;
import firok.spring.jfb.util.NativeProcess;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;

public class FFmpegTranscodeService
{
	public FFmpegTranscodeService()
	{
		if(!PlatformType.Windows.isCurrent())
		{
			throw new RuntimeException("目前只适配 Windows");
		}
	}

	@Value("${app.lib.ffmpeg-base.path-ffmpeg}")
	public String pathFFmpeg;

	@Value("${app.lib.ffmpeg-base.path-ffprobe}")
	public String pathFFprobe;

	/**
	 * ffmpeg 版本信息
	 */
	public static String versionFFmpeg = null;

	@PostConstruct
	private void postConstruct()
	{
		synchronized (FFmpegTranscodeService.class)
		{
			if(versionFFmpeg != null) return;

			// todo 1 这里目前是基于cmd 后面可能会出现操作系统相关的内容
			// todo 2 这里的ffmpeg路径是写死的 后面估计会换成上面配置项里的值
			try(var process = new NativeProcess(pathFFmpeg + " -version"))
			{
				int value = process.waitFor();

				var contentOut = process.contentOut();
				var contentErr = process.contentErr();

				if(value != 0 || !contentErr.isBlank()) // 错误输出流有内容
				{
					System.err.println("读取 ffmpeg 版本信息时获取到错误信息:\n====");
					System.err.println(contentErr);
					System.err.println("====");

					throw new RuntimeException("读取 ffmpeg 版本信息出错");
				}
				else if(!contentOut.isBlank()) // 标准输出流有内容
				{
					var lines = contentOut.split("\n");
					System.out.println("读取 ffmpeg 版本信息:\n====");
					System.out.println(versionFFmpeg = lines[0]);
					System.out.println("====");
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException("没有检测到可用 ffmpeg, 请确保系统中已正确安装依赖或正确配置可执行文件路径", e);
			}
		}
	}
}
