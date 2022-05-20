package firok.spring.jfb.service_impl.storage;

import com.qiniu.common.QiniuException;
import com.qiniu.storage.DownloadUrl;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import firok.spring.jfb.bean.Ret;
import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.storage.IStorageIntegrative;
import firok.spring.jfb.service_impl.ContextKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;

/**
 * 基于七牛云的数据持久化实现
 */
//@ConditionalOnExpression("${app.service-storage.qiniu.enable}")
@Service
public class QiniuStorageService implements IStorageIntegrative, IWorkflowService
{
	public static final String SERVICE_NAME = ContextKeys.PREFIX + "qiniu-storage";

	@Value("${app.service-storage.qiniu.access-key}")
	public String accessKey;

	@Value("${app.service-storage.qiniu.secret-key}")
	public String secretKey;

	record BucketInfo(
			String nameBucket,
			String domain,
			Region region,
			boolean useHttps,
			int deadline,
			UploadManager uploadManager
	) { }

	Map<String, BucketInfo> mapBuckets;
	@Value("${app.service-storage.qiniu.buckets}")
	public void setMapBuckets(Map<String, Map<String, String>> configBucket)
	{
		for(var entry : configBucket.entrySet())
		{
			var nameBucket = entry.getKey();
			var infoBucket = entry.getValue();
			var domain = String.valueOf(infoBucket.get("domain"));
			var region = getRegionByName(String.valueOf(infoBucket.get("region")));
			var useHttps = Boolean.parseBoolean(String.valueOf(infoBucket.get("useHttps")));
			var deadline = Integer.parseInt(String.valueOf(infoBucket.get("deadline")));
			var cfg = new com.qiniu.storage.Configuration(Region.huadong());
			var managerUpload = new UploadManager(cfg);
			var info = new BucketInfo(nameBucket, domain, region, useHttps, deadline, managerUpload);
			mapBuckets.put(nameBucket, info);
		}
	}
	BucketInfo getBucket(String nameBucket)
	{
		var bucketInfo = mapBuckets.get(nameBucket);
		if(bucketInfo == null)
			throw new IllegalArgumentException("找不到指定桶, 请在配置文件中配置");
		return bucketInfo;
	}

	public Auth auth;
	@PostConstruct
	protected void postConstruct()
	{
		auth = Auth.create(accessKey, secretKey);
	}

	protected static Region getRegionByName(String name)
	{
		return switch (name)
		{
			case "huadong" -> Region.huadong();
			case "huadongZheJiang2" -> Region.huadongZheJiang2();
			case "qvmHuadong" -> Region.qvmHuadong();
			case "huabei" -> Region.huabei();
			case "qvmHuabei" -> Region.qvmHuabei();
			case "huanan" -> Region.huanan();
			case "beimei" -> Region.beimei();
			case "xinjiapo" -> Region.xinjiapo();
			case "fogCnEast1" -> Region.regionFogCnEast1();
			default -> Region.autoRegion(); // 默认用自动域名
		};
	}

	@SuppressWarnings("HttpUrlsUsage")
	public String urlPrivate(String nameBucket, String nameFile, boolean withPM3U8)
	{
		var bucketInfo = getBucket(nameBucket);
		var urlOrigin = MessageFormat.format(
				"http://{0}/{1}{2}",
				bucketInfo.domain,
				nameFile,
				withPM3U8 ? "?pm3u8/0/expires/43200" : ""
		);
		return auth.privateDownloadUrl(urlOrigin, bucketInfo.deadline);
	}

	@SuppressWarnings("HttpUrlsUsage")
	public String urlPublic(String nameBucket, String nameFile)
	{
		var bucketInfo = getBucket(nameBucket);
		return MessageFormat.format(
				"http://{0}/{1}",
				bucketInfo.domain,
				nameFile
		);
	}

	@Override
	public void store(String nameBucket, String nameObject, InputStream is) throws ExceptionIntegrative
	{
		try
		{
			var bucketInfo = getBucket(nameBucket);
			var token = auth.uploadToken(nameBucket);
			var params = new StringMap();
			bucketInfo.uploadManager.put(is, nameObject, token, params, MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE);
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
			var bucketInfo = getBucket(nameBucket);
			var du = new DownloadUrl(bucketInfo.domain, bucketInfo.useHttps, nameObject);
			var du2 = du.buildURL(auth, bucketInfo.deadline);
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

	@Override
	public String getWorkflowServiceOperation()
	{
		return SERVICE_NAME;
	}

	@Override
	public Map<String, Class<?>> getWorkflowParamContext()
	{
		var ret = IWorkflowService.super.getWorkflowParamContext();
		ret.put(ContextKeys.KEY_FILES, File[].class);
		ret.put(ContextKeys.KEY_NAME_BUCKET, String.class);
		return ret;
	}

	@Override
	public void operateWorkflow(WorkflowContext context) throws ExceptionIntegrative
	{
		StorageTransferUtil.transfer(this, context);
	}
}
