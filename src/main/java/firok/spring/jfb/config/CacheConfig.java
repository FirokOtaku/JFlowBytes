package firok.spring.jfb.config;

import firok.spring.jfb.ioo.rdo.CacheFolder;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

@Deprecated
@Configuration
public class CacheConfig
{
	/**
	 * 缓存根目录
	 */
	@Value("${app.cache.folder}")
	File fileFolderCache;

	/**
	 * 根据指定名称, 创建一个缓存目录
	 *
	 * fixme 创建文件夹的逻辑放这不太合适 后面挪挪位置
	 * @param name 缓存目录名称
	 * @throws IOException 创建目录出现问题
	 */
	public CacheFolder createCacheFolder(String name) throws IOException
	{
		var folderRoot = new File(fileFolderCache, name);
		// 创建切片文件夹
		var folderSlice = new File(folderRoot, "slice");
		FileUtils.forceMkdir(folderSlice);
		// 创建转码切片文件夹
		var folderTransform = new File(folderRoot, "transform");
		FileUtils.forceMkdir(folderTransform);
		// 创建合并文件
		var fileMerge = new File(folderRoot, "merge.bin");
		fileMerge.createNewFile();
		return new CacheFolder(folderRoot, folderSlice, fileMerge, folderTransform);
	}

}
