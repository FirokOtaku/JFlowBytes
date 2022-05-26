package firok.spring.jfb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

import javax.annotation.PostConstruct;
import java.util.Properties;

@SpringBootApplication
@MapperScan("firok.spring.jfb.mapper")
public class JFlowBytesApplication
{
	public static String name;
	public static String version;
	public static String description;
	public static String author;
	public static String url;
	static
	{
		try(var ios = ClassLoader.getSystemResourceAsStream("project.properties"))
		{
			Properties props = new Properties();
			props.load(ios);
			name = props.getProperty("project.name");
			version = props.getProperty("project.version");
			description = props.getProperty("project.description");
			author = props.getProperty("project.author");
			url = props.getProperty("project.url");
		}
		catch (Exception e)
		{
			name = "jfb";
			version = "0.1.x";
			description = "A simple file processing system.";
			author = "Firok";
			url = "https://github.com/351768593/Pivi";
		}
	}

	@PostConstruct
	public void postConstruct()
	{
		System.out.printf("%s v%s by %s\n", name, version, author);
	}

	static org.springframework.context.ConfigurableApplicationContext contextSpringBoot;

	public static void stop()
	{
		contextSpringBoot.close();
	}

	public static void main(String[] args)
	{
		contextSpringBoot = SpringApplication.run(JFlowBytesApplication.class, args);
	}

}
