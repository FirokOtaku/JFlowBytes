package firok.spring.jfb.service_impl;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.DownloadUrl;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * 与七牛云 OSS 储存交互
 */
//@Service
@Deprecated
public class QiniuStorageService
{
	static final String accessKey = "K4AyEBRYQYDTY5Gx7BPIsQQib1aIE4MwXD7b83_P";
	static final String secretKey = "lllhjhaQtIvXJXBo540Miw2VZzqT9Olf1o48kx6N";
	static final String bucket = "demo-firok";

	Auth auth;
	String tokenUpload;
	UploadManager managerUpload;
	public QiniuStorageService()
	{
		System.out.println("初始化七牛云上传服务");
		auth = Auth.create(accessKey, secretKey);
		tokenUpload = auth.uploadToken(bucket);
		System.out.println("获取到上传凭证: " + tokenUpload);

		//构造一个带指定Region对象的配置类
		var cfg = new Configuration(Region.huadong());
		managerUpload = new UploadManager(cfg);
	}

	public DefaultPutRet upload(File file) throws FileNotFoundException, QiniuException
	{
		var res = managerUpload.put(file, file.getName(), tokenUpload);
		return res.jsonToObject(DefaultPutRet.class);
	}

	public String urlPrivate(String key) throws QiniuException
	{
		var url = new DownloadUrl("rb2uaos6l.hd-bkt.clouddn.com", false, key);
		return url.buildURL(auth, 3600);
	}

}
