package firok.spring.jfb.service_impl;


import firok.spring.jfb.service.IStorageIntegrative;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@ConditionalOnExpression("'file-system'.equals('${app.service-storage.type}')")
@Service
public class FileSystemStorageService implements IStorageIntegrative
{
	public FileSystemStorageService()
	{
		System.out.println("初始化咯");
	}

	@Override
	public boolean store(String id, InputStream is)
	{
		return false;
	}
}
