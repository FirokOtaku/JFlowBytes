package firok.spring.jfb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaticController
{
	/**
	 * 虽然没什么用 单纯为了刷新页面的时候控制台少一条没用的报错
	 */
	@SuppressWarnings("SpringMVCViewInspection")
	@GetMapping("/favicon.ico")
	public String favicon()
	{
		return "/static/favicon.ico";
	}

	@GetMapping("/")
	public String index()
	{
		return "redirect:/static/flow-demo.html";
	}
}
