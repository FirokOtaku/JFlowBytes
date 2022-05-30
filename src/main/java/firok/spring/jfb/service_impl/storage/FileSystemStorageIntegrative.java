package firok.spring.jfb.service_impl.storage;


import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.storage.IStorageIntegrative;
import firok.spring.jfb.constant.ContextKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.util.Map;

import static firok.spring.jfb.constant.ContextKeys.KEY_FILES;

//@ConditionalOnExpression("${app.service-storage.file-system.enable}")
@Service
@ThreadSafe
public class FileSystemStorageIntegrative implements IStorageIntegrative, IWorkflowService
{
	public static final String SERVICE_NAME = ContextKeys.PREFIX + "filesystem-storage";

	@Value("${app.service-storage.file-system.folder-storage}")
	public File folderStorage;

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
		var fileObject = new File(folderBucket, nameObject);
		folderBucket.mkdirs();

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
		var fileObject = new File(folderBucket, nameObject);

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
}
