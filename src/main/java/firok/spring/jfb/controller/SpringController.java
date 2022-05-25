package firok.spring.jfb.controller;

import firok.spring.jfb.JFlowBytesApplication;
import firok.spring.jfb.bean.Ret;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spring")
public class SpringController
{
	boolean toBeStop = false;
	@GetMapping("/stop")
	public Ret<?> stop()
	{
		synchronized (this)
		{
			if(toBeStop) return Ret.success("正在准备停止应用");

			new Thread(()->{
				try { Thread.sleep(500); }
				catch (Exception ignored) { }

				JFlowBytesApplication.stop();
			}).start();

			toBeStop = true;
			return Ret.success("应用正在停止");
		}
	}
}
