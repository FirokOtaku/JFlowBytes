package firok.spring.jfb.service.transcode;

import java.io.File;

/**
 * 将一个视频文件转码为 M3U8 格式视频文件列表
 */
public interface ITranscodeM3U8Integrative
{
	/**
	 * 将一个视频文件转换为 M3U8 格式文件列表
	 * @param fileVideo 将要转换的视频文件
	 * @return 转换后的文件列表
	 */
	File[] toM3U8(File fileVideo);
}
