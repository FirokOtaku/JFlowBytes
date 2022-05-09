package firok.spring.jfb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@ConditionalOnExpression("${app.service-upload.enable}")
@Configuration
public class UploadConfig
{
	@Value("${app.service-upload.folder-cache}")
	File folderCache;
}
