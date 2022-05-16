package firok.spring.jfb.service_impl.storage;


import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.storage.IStorageIntegrative;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.util.Map;

import static firok.spring.jfb.service_impl.ContextKeys.KEY_FILES;

//@ConditionalOnExpression("${app.service-storage.file-system.enable}")
@Service
@ThreadSafe
public class FileSystemStorageService implements IStorageIntegrative, IWorkflowService
{
	@Value("${app.service-storage.file-system.folder-storage}")
	public File folderStorage = new File("./storage");

	@Override
	public String getWorkflowServiceOperation()
	{
		return "jfb:file-system-storage";
	}

	@Override
	public Map<String, Class<?>> getWorkflowParamContext()
	{
		var ret = IWorkflowService.super.getWorkflowParamContext();
		ret.put(KEY_FILES, File[].class);
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
		// gossip 那么文件列表长度为0的情况要不要做处理是个问题
		var listFile = context.get(KEY_FILES) instanceof File[] files ? files : new File[0];

		for (var file : listFile)
		{
			storeByFile("", file);
		}
	}
}
