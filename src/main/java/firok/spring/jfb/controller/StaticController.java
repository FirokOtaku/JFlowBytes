package firok.spring.jfb.controller;

import firok.spring.jfb.JFlowBytesApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 静态资源相关
 */
@Controller
public class StaticController
{
	@RequestMapping("/")
	public String index(Model model)
	{
		model.addAttribute("project_name", JFlowBytesApplication.name);
		model.addAttribute("project_version", JFlowBytesApplication.version);
		return "index.html";
	}

	@RequestMapping("/web_uploader")
	public String web_uploader_index() { return "redirect:/web_uploader/index.html"; }

	@RequestMapping("/web_uploader/index.html")
	public String web_uploader_indexHtml() { return "/static/web_uploader/index.html"; }

	@RequestMapping("/web_uploader/favicon.ico")
	public String web_uploader_favicon() { return "/static/web_uploader/favicon.ico"; }

	@RequestMapping("/web_uploader/js/app.js")
	public String web_uploader_appJs() { return "/static/web_uploader/js/app.js"; }

	@RequestMapping("/web_uploader/js/chunk-vendors.js")
	public String web_uploader_chunkVendorsJs() { return "/static/web_uploader/js/chunk-vendors.js"; }

}
