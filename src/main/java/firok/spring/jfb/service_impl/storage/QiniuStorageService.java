package firok.spring.jfb.service_impl.storage;

import com.qiniu.storage.DownloadUrl;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.storage.IStorageIntegrative;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * 基于七牛云的数据持久化实现
 */
@ConditionalOnExpression("${app.service-storage.qiniu.enable}")
@Service
public class QiniuStorageService implements IStorageIntegrative
{
	@Value("${app.service-storage.qiniu.access-key}")
	public String accessKey;

	@Value("${app.service-storage.qiniu.secret-key}")
	public String secretKey;

	@Value("getRegionByName('${app.service-storage.qiniu.region}')")
	public Region nameRegion;

	public Auth auth;
	public UploadManager managerUpload;

	@Value("${app.service-storage.qiniu.domain}")
	public String domain;

	@Value("${app.service-storage.qiniu.use-https}")
	public boolean useHttps;

	@Value("${app.service-storage.qiniu.deadline}")
	public int deadline;

	@PostConstruct
	protected void postConstruct()
	{
		auth = Auth.create(accessKey, secretKey);
		var cfg = new com.qiniu.storage.Configuration(Region.huadong());
		managerUpload = new UploadManager(cfg);
	}

	protected static Region getRegionByName(String name)
	{
		return switch (name)
				{
					case "huadong" -> Region.huadong();
					case "huabei" -> Region.huabei();
					// todo 更多区域
					default -> null;
				};
	}

	@Override
	public void store(String nameBucket, String nameObject, InputStream is) throws ExceptionIntegrative
	{
		try
		{
			var token = auth.uploadToken(nameBucket);
			var params = new StringMap();
			managerUpload.put(is, nameObject, token, params, MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE);
		}
		catch (Exception e)
		{
			throw new ExceptionIntegrative(e);
		}
	}

	@Override
	public void extract(String nameBucket, String nameObject, OutputStream os) throws ExceptionIntegrative
	{
		try
		{
			var du = new DownloadUrl(domain, useHttps, nameObject);
			var du2 = du.buildURL(auth, deadline);
			var url = new URL(du2);
			try(var is = url.openStream())
			{
				is.transferTo(os);
			}
		}
		catch (Exception e)
		{
			throw new ExceptionIntegrative(e);
		}
	}
}
