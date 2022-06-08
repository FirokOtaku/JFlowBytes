package firok.spring.jfb.config;

import firok.spring.jfb.controller.FlowController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Configuration
@EnableScheduling
public class FlowTimeoutConfig
{
	@Autowired
	public FlowController flows;

	/**
	 * 定期清理超时工作流
	 */
	@Scheduled(fixedRate = 60_000, initialDelay = 60_000)
	public void cleanTimeoutWorkflow()
	{
		flows.cleanTimeoutWorkflow();
	}
}
