package firok.spring.jfb.service_impl.transcode;

import firok.spring.jfb.config.FFmpegTranscodeConfig;
import firok.spring.jfb.service.transcode.ITranscodeM3U8Integrative;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 调用 FFmpeg 把单视频文件转码成 M3U8 视频文件列表
 * @implNote 启用这个 service 需要保证配置文件同时启用 ffmpeg 和转码服务
 */
@ConditionalOnBean(FFmpegTranscodeConfig.class)
@ConditionalOnExpression("${app.service-transcode.ffmpeg-m3u8.enable}")
@Service
public class FFmpegTranscodeM3U8Service implements ITranscodeM3U8Integrative
{
	@Override
	public File[] toM3U8(File fileVideo)
	{
		return new File[0];
	}
}
