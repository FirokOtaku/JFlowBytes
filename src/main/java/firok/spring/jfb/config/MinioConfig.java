package firok.spring.jfb.config;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class MinioConfig
{
	@Value("${app.minio.name-bucket}")
	public String nameBucket;

	@Value("${app.minio.url}")
	public String url;

	@Value("${app.minio.username}")
	public String username;

	@Value("${app.minio.password}")
	public String password;

	@Bean
	public MinioClient minioClient() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException
	{
		System.out.println("连接MinIO...");
		var ret = MinioClient.builder()
				.endpoint(url)
				.credentials(username, password)
				.build();
		System.out.println("连接成功");
		var be = ret.bucketExists(BucketExistsArgs.builder().bucket(nameBucket).build());
		System.out.println("检查存在默认bucket: "+be);
		if(!be)
		{
			ret.makeBucket(MakeBucketArgs.builder().bucket(nameBucket).build());
			System.out.println("创建默认bucket完成");
		}
		return ret;
	}
}
