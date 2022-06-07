package firok.spring.jfb.service_impl.storage;

import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.storage.IStorageIntegrative;
import firok.spring.jfb.constant.ContextKeys;
import io.minio.*;
import io.minio.messages.DeleteObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * 基于 MinIO 的数据持久化实现
 */
@ConditionalOnExpression("${app.service-storage.minio.enable}")
@Service
public class MinioStorageIntegrative implements IStorageIntegrative, IWorkflowService
{
	public static final String SERVICE_NAME = ContextKeys.PREFIX + "minio" + STORAGE_SERVICE_SUFFIX;

	@Value("${app.service-storage.minio.url}")
	public String url;

	@Value("${app.service-storage.minio.username}")
	public String username;

	@Value("${app.service-storage.minio.password}")
	public String password;

	@Value("${app.service-storage.minio.auto-make-bucket}")
	public boolean isAutoMakeBucket;

	public MinioClient client;

	@PostConstruct
	protected void connectMinio()
	{
		client = MinioClient.builder()
				.endpoint(url)
				.credentials(username, password)
				.build();
	}

	@Override
	public void store(String nameBucket, String nameObject, InputStream is) throws ExceptionIntegrative
	{
		// 上传之前先检查一下桶是否存在
		if(isAutoMakeBucket)
		{
			var argsBucketExist = BucketExistsArgs.builder().bucket(nameBucket).build();
			boolean isBucketExist;
			try
			{
				isBucketExist = client.bucketExists(argsBucketExist);
			}
			catch (Exception e)
			{
				throw new ExceptionIntegrative("无法检查桶存在性", e);
			}

			if(!isBucketExist) // 桶不存在 尝试创建桶
			{
				try
				{
					var argsMakeBucket = MakeBucketArgs.builder().bucket(nameBucket).build();
					client.makeBucket(argsMakeBucket);
				}
				catch (Exception e)
				{
					throw new ExceptionIntegrative("无法创建桶", e);
				}
			}
		}

		var args = PutObjectArgs.builder()
				.bucket(nameBucket)
				.object(nameObject)
				.stream(is, -1, 5242880) // todo 这里可能需要开放配置出去
				.contentType(MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE)
				.build();
		try
		{
			client.putObject(args);
		}
		catch (Exception e)
		{
			throw new ExceptionIntegrative(e);
		}
	}

	@Override
	public void extract(String nameBucket, String nameObject, OutputStream os) throws ExceptionIntegrative
	{
		var args = GetObjectArgs.builder()
				.bucket(nameBucket)
				.object(nameObject)
				.build();
		try
		{
			var ret = client.getObject(args);
			ret.transferTo(os);
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
			if(namesObject.length == 1)
			{
				var nameObject = namesObject[0];
				var args = io.minio.RemoveObjectArgs.builder()
						.bucket(nameBucket)
						.object(nameObject)
						.build();
				client.removeObject(args);
			}
			else
			{
				var list = new ArrayList<DeleteObject>();
				for(var nameObject : namesObject)
				{
					list.add(new DeleteObject(nameObject));
				}
				var args = io.minio.RemoveObjectsArgs.builder()
						.bucket(nameBucket)
						.objects(list)
						.build();
				client.removeObjects(args);
			}

		}
		catch (Exception e)
		{
			throw new ExceptionIntegrative("从 MinIO 删除对象时发生错误", e);
		}
	}
}
