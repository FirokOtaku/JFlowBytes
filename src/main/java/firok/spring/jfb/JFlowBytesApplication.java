package firok.spring.jfb;

import firok.topaz.Topaz;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.util.*;

@SpringBootApplication
@MapperScan("firok.spring.jfb.mapper")
public class JFlowBytesApplication
{
	public static JFlowBytesApplication instance;
	public JFlowBytesApplication() { instance = this; }

	public static String name;
	public static String version;
	public static String description;
	public static String author;
	public static String url;
	static
	{
		try(var ios = new ClassPathResource("project.properties").getInputStream())
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
		System.out.printf("Hi there, Topaz %s, it's me, %s %s%n", Topaz.VERSION, name, version);
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
