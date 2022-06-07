package firok.spring.jfb;

import firok.spring.jfb.controller.FlowController;
import firok.topaz.Topaz;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.util.*;

@SpringBootApplication
@EnableScheduling
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
//		System.out.printf("%s v%s by %s\n", name, version, author);
		System.out.println("Hi there, Topaz " + Topaz.VERSION);
	}

	static org.springframework.context.ConfigurableApplicationContext contextSpringBoot;
	public static void stop()
	{
		contextSpringBoot.close();
	}

	@Autowired
	public FlowController flows;

	/**
	 * 定期清理超时工作流
	 */
	@Scheduled(fixedRate = 60_000, initialDelay = 1_000)
	public void cleanTimeoutWorkflow()
	{
		flows.cleanTimeoutWorkflow();
	}

	public static void main(String[] args)
	{
		contextSpringBoot = SpringApplication.run(JFlowBytesApplication.class, args);
	}

}
