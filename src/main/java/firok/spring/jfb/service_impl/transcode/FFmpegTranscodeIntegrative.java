package firok.spring.jfb.service_impl.transcode;

import firok.topaz.platform.NativeProcess;
import firok.topaz.platform.PlatformTypes;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;

public class FFmpegTranscodeIntegrative
{
	public FFmpegTranscodeIntegrative()
	{
		if(!PlatformTypes.Windows.isCurrent())
		{
			throw new RuntimeException("目前只适配 Windows");
		}
	}

	@Value("${app.service-transcode.ffmpeg-base.path-ffmpeg}")
	public String pathFFmpeg;

	@Value("${app.service-transcode.ffmpeg-base.path-ffprobe}")
	public String pathFFprobe;

	/**
	 * ffmpeg 版本信息
	 */
	public static String versionFFmpeg = null;

	/**
	 * ffprobe 版本信息
	 */
	public static String versionFFprobe = null;

	@PostConstruct
	private void postConstruct()
	{
		synchronized (FFmpegTranscodeIntegrative.class)
		{
			if(versionFFmpeg != null) return;
			else
			{
				// todo 这里目前是基于cmd 后面可能会出现操作系统相关的内容
				try(var process = new NativeProcess(pathFFmpeg + " -version"))
				{
					int ret = process.waitFor();

					if(ret != 0)
					{
						System.err.println("读取 ffmpeg 版本信息时获取到错误信息:\n====");
						System.err.println(process.contentErr());
						System.err.println("====");

						throw new RuntimeException("读取 ffmpeg 版本信息出错");
					}
					else
					{
						System.out.println("ffmpeg 版本信息: " + (versionFFmpeg = process.contentOut().split("\n")[0]));
					}
				}
				catch (Exception e)
				{
					throw new RuntimeException("没有检测到可用 ffmpeg, 请确保系统中已正确安装依赖或正确配置可执行文件路径", e);
				}
			}

			if(versionFFprobe != null) return;
			else
			{
				try(var process = new NativeProcess(pathFFprobe + " -version"))
				{
					int ret = process.waitFor();

					if(ret != 0)
					{
						System.err.println("读取 ffprobe 版本信息时获取到错误信息:\n====");
						System.err.println(process.contentErr());
						System.err.println("====");
					}
					else
					{
						System.out.println("ffprobe 版本信息: " + (versionFFprobe = process.contentOut().split("\n")[0]));
					}
				}
				catch (Exception e)
				{
					throw new RuntimeException("没有检测到可用 ffprobe, 请确保系统中已正确安装依赖或正确配置可执行文件路径", e);
				}
			}
		}
	}
}
