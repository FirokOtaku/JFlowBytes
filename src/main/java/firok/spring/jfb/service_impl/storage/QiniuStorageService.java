package firok.spring.jfb.service_impl.storage;

import com.qiniu.storage.DownloadUrl;
import com.qiniu.util.StringMap;
import firok.spring.jfb.config.QiniuStorageConfig;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.storage.IStorageIntegrative;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * 基于七牛云的数据持久化实现
 */
@ConditionalOnBean(QiniuStorageConfig.class)
@Service
public class QiniuStorageService implements IStorageIntegrative
{
	@Autowired
	QiniuStorageConfig config;

	@Override
	public void store(String nameBucket, String nameObject, InputStream is) throws ExceptionIntegrative
	{
		try
		{
			var token = config.auth.uploadToken(nameBucket);
			var params = new StringMap();
			config.managerUpload.put(is, nameObject, token, params, MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE);
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
			var du = new DownloadUrl(config.domain, config.useHttps, nameObject);
			var du2 = du.buildURL(config.auth, config.deadline);
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
