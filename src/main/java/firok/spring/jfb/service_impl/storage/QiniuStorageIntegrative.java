package firok.spring.jfb.service_impl.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.qiniu.storage.*;
import com.qiniu.storage.model.BatchStatus;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import firok.spring.jfb.config.BucketInfo;
import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.storage.IStorageIntegrative;
import firok.spring.jfb.constant.ContextKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于七牛云的数据持久化实现
 */
@ConditionalOnExpression("${app.service-storage.qiniu.enable}")
@Service
public class QiniuStorageIntegrative implements IStorageIntegrative, IWorkflowService
{
	public static final String SERVICE_NAME = ContextKeys.PREFIX + "qiniu" + STORAGE_SERVICE_SUFFIX;

	@Value("${app.service-storage.qiniu.access-key}")
	public String accessKey;

	@Value("${app.service-storage.qiniu.secret-key}")
	public String secretKey;

	Map<String, BucketInfo> mapBuckets;
	@Value("${app.service-storage.qiniu.buckets}")
	public void setMapBuckets(String raw) throws JsonProcessingException, IllegalArgumentException
	{
		mapBuckets = new HashMap<>();
		var om = new ObjectMapper();
		// todo low 这里有可能出现json格式不对的情况 最好是做一下控制 但是现在没空写
		var json = (ObjectNode) om.readTree(raw);
		var iterElements = json.fieldNames();
		while(iterElements.hasNext())
		{
			var nameBucket = iterElements.next();
			var obj = (ObjectNode) json.get(nameBucket);
			var domain = obj.get("domain").asText();
			var region = getRegionByName(obj.get("region").asText());
			var useHttps = obj.get("use-https").asBoolean();
			var deadline = obj.get("deadline").asInt();
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
				bucketInfo.domain(),
				nameFile,
				withPM3U8 ? "?pm3u8/0/expires/43200" : ""
		);
		return auth.privateDownloadUrl(urlOrigin, bucketInfo.deadline());
	}

	@SuppressWarnings("HttpUrlsUsage")
	public String urlPublic(String nameBucket, String nameFile)
	{
		var bucketInfo = getBucket(nameBucket);
		return MessageFormat.format(
				"http://{0}/{1}",
				bucketInfo.domain(),
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
			bucketInfo.uploadManager().put(is, nameObject, token, params, MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE);
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
			var du = new DownloadUrl(bucketInfo.domain(), bucketInfo.useHttps(), nameObject);
			var du2 = du.buildURL(auth, bucketInfo.deadline());
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

	@Override
	public void delete(String nameBucket, String... namesObject) throws ExceptionIntegrative
	{
		try
		{
			var bucketInfo = getBucket(nameBucket);
			var region = bucketInfo.region();
			var config = new Configuration(region);
			var bmgr = new BucketManager(auth, config);
			if(namesObject.length == 1)
			{
				bmgr.delete(nameBucket, namesObject[0]);
			}
			else
			{
				// todo high 长度大于1000的话需要切分
				// https://developer.qiniu.com/kodo/1239/java#rs-batch-delete
				// @see firok.topaz.Collections 在其它项目用到过这个代码
				var batchOperations = new BucketManager.BatchOperations()
						.addDeleteOp(nameBucket, namesObject);
				var response = bmgr.batch(batchOperations);
				var batchStatusList = response.jsonToObject(BatchStatus[].class);
				for (var status : batchStatusList)
				{
					if (status.code != 200)
						throw new RuntimeException("从七牛云储存删除对象时发生错误: "+status.data.error);
				}
			}
			// todo 删除错误的话得重试才行
		}
		catch (Exception e)
		{
			throw new ExceptionIntegrative(e);
		}
	}
}
