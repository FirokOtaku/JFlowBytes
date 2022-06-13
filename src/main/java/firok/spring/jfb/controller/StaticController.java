package firok.spring.jfb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 静态资源相关
 */
@Controller
public class StaticController
{
	@RequestMapping("/favicon.ico")
	public String favicon()
	{
		return "/static/favicon.ico";
	}
}
