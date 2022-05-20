package firok.spring.jfb.controller;

import com.qiniu.common.QiniuException;
import firok.spring.jfb.service_impl.storage.QiniuStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.MessageFormat;

@ConditionalOnBean(QiniuStorageService.class)
@RestController
@RequestMapping("/api/storage/qiniu")
public class QiniuStorageController
{
	@Autowired
	QiniuStorageService service;

	@GetMapping("/space_private/{nameBucket}/{nameFile}")
	public String spacePrivate(
			@PathVariable("nameBucket") String nameBucket,
			@PathVariable("nameFile") String nameFile
	) throws QiniuException
	{
		return service.urlPrivate(nameFile);
	}

	@SuppressWarnings("HttpUrlsUsage")
	@GetMapping("/space_public/{nameBucket}/{nameFile}")
	public String spacePublic(
			@PathVariable("nameBucket") String nameBucket,
			@PathVariable("nameFile") String nameFile
	)
	{
		return MessageFormat.format(
				"http://{0}/{1}",
				service.domain,
				nameFile
		);
	}

}
