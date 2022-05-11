package firok.spring.jfb.service_impl.storage;


import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.storage.IStorageIntegrative;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import javax.annotation.concurrent.ThreadSafe;
import java.io.*;

@ConditionalOnExpression("${app.service-storage.type}")
@Service
@ThreadSafe
public class FileSystemStorageService implements IStorageIntegrative
{
	@Value("${app.service-storage.file-system.enable}")
	public File folderStorage;

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
}
