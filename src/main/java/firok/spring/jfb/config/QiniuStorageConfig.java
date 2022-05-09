package firok.spring.jfb.config;

import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@ConditionalOnExpression("${app.service-storage.qiniu.enable}")
@Configuration
public class QiniuStorageConfig
{
	@Value("${app.service-storage.qiniu.access-key}")
	public String accessKey;

	@Value("${app.service-storage.qiniu.secret-key}")
	public String secretKey;

	@Value("getRegionByName('${app.service-storage.qiniu.region}')")
	public Region nameRegion;

	public Auth auth;
	public UploadManager managerUpload;

	@Value("${app.service-storage.qiniu.domain}")
	public String domain;

	@Value("${app.service-storage.qiniu.use-https}")
	public boolean useHttps;

	@Value("${app.service-storage.qiniu.deadline}")
	public int deadline;

	@PostConstruct
	protected void postConstruct()
	{
		auth = Auth.create(accessKey, secretKey);
		var cfg = new com.qiniu.storage.Configuration(Region.huadong());
		managerUpload = new UploadManager(cfg);
	}

	protected static Region getRegionByName(String name)
	{
		return switch (name)
		{
			case "huadong" -> Region.huadong();
			case "huabei" -> Region.huabei();
			// todo 更多区域
			default -> null;
		};
	}
}
