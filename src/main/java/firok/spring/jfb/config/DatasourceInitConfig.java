package firok.spring.jfb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * 数据库初始化配置
 */
@Configuration
public class DatasourceInitConfig
{
	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
	@Bean
	public DataSourceInitializer dataSourceInitializer(final DataSource dataSource)
	{
		var rdp = new ResourceDatabasePopulator();
		rdp.addScript(new ClassPathResource("/data.sql"));

		var dsi = new DataSourceInitializer();
		dsi.setDataSource(dataSource);
		dsi.setDatabasePopulator(rdp);
		return dsi;
	}
}
