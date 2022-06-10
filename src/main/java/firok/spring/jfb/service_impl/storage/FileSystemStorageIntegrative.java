package firok.spring.jfb.service_impl.storage;


import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.hash.IHashMapper;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.storage.IStorageIntegrative;
import firok.spring.jfb.constant.ContextKeys;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.util.Map;

import static firok.spring.jfb.constant.ContextKeys.KEY_FILES;

@ConditionalOnExpression("${app.service-storage.file-system.enable}")
@Service
@ThreadSafe
public class FileSystemStorageIntegrative implements IStorageIntegrative, IWorkflowService
{
	public static final String SERVICE_NAME = ContextKeys.PREFIX + "filesystem" + STORAGE_SERVICE_SUFFIX;

	@Override
	public String getStorageTargetName()
	{
		return "filesystem";
	}

	@Value("${app.service-storage.file-system.folder-storage}")
	public File folderStorage;

	public IHashMapper<?> mapperHash;
	@Value("${app.service-storage.file-system.filename-hash-mapper:no-hash}")
	public void setMapperHash(String name)
	{
		var mapper = IHashMapper.getMapper(name);
		if(mapper == null)
			throw new IllegalArgumentException("找不到指定文件路径映射器: " + name);
		this.mapperHash = mapper;
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
		ret.put(KEY_FILES, File[].class);
		ret.put(ContextKeys.KEY_NAME_BUCKET, String.class);
		return ret;
	}

	@Override
	public void store(String nameBucket, String nameObject, InputStream is) throws ExceptionIntegrative
	{
		var folderBucket = new File(folderStorage, nameBucket);
		var hash = this.mapperHash.mapHash(nameObject);
		var fileObject = new File(folderBucket, hash.getHashString());
		fileObject.getParentFile().mkdirs();

		try(var ofs = new FileOutputStream(fileObject))
		{
			is.transferTo(ofs);
		}
		catch (IOException e)
		{
			throw new ExceptionIntegrative(e);
		}
	}

	@Override
	public void extract(String nameBucket, String nameObject, OutputStream os) throws ExceptionIntegrative
	{
		var folderBucket = new File(folderStorage, nameBucket);
		var hash = this.mapperHash.mapHash(nameObject);
		var fileObject = new File(folderBucket, hash.getHashString());

		try
		{
			var ifs = new FileInputStream(fileObject);
			ifs.transferTo(os);
		}
		catch (IOException e)
		{
			throw new ExceptionIntegrative(e);
		}
	}

	@Override
	public void operateWorkflow(WorkflowContext context) throws ExceptionIntegrative
	{
		StorageTransferUtil.transfer(this, context);
	}

	@Override
	public void delete(String nameBucket, String... namesObject) throws ExceptionIntegrative
	{
		var folderBucket = new File(folderStorage, nameBucket);
		for(var nameObject : namesObject)
		{
			var hash = this.mapperHash.mapHash(nameObject);
			var fileObject = new File(folderBucket, hash.getHashString());
			try
			{
				FileUtils.forceDelete(fileObject);
			}
			catch (Exception e)
			{
				throw new ExceptionIntegrative("从本地储存删除文件时发生错误: " + e.getMessage(), e);
			}
		}
	}
}
