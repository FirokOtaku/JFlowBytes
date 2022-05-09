package firok.spring.jfb.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@ConditionalOnExpression("${app.service-storage.minio.enable}")
@Configuration
public class MinioStorageConfig
{
	@Value("${app.service-storage.minio.url}")
	public String url;

	@Value("${app.service-storage.minio.username}")
	public String username;

	@Value("${app.service-storage.minio.password}")
	public String password;

	public MinioClient client;

	@PostConstruct
	protected void connectMinio()
	{
		client = MinioClient.builder()
				.endpoint(url)
				.credentials(username, password)
				.build();
	}
}
