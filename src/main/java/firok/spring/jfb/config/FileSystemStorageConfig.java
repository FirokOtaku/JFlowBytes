package firok.spring.jfb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@ConditionalOnExpression("${app.service-storage.type}")
@Configuration
public class FileSystemStorageConfig
{
	@Value("${app.service-storage.file-system.enable}")
	public File folderStorage;
}
