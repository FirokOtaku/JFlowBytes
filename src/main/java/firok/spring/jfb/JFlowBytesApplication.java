package firok.spring.jfb;

import firok.spring.jfb.service.IStorageIntegrative;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("firok.spring.jfb.mapper")
public class JFlowBytesApplication
{

	public static void main(String[] args)
	{
		SpringApplication.run(JFlowBytesApplication.class, args);
	}

}
