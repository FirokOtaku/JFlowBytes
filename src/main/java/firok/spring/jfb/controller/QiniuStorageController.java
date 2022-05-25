package firok.spring.jfb.controller;

import com.qiniu.common.QiniuException;
import firok.spring.jfb.bean.Ret;
import firok.spring.jfb.service_impl.storage.QiniuStorageIntegrative;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnBean(QiniuStorageIntegrative.class)
@RestController
@RequestMapping("/api/storage/qiniu")
public class QiniuStorageController
{
	@Autowired
	QiniuStorageIntegrative service;

	@GetMapping("/space_private/{nameBucket}/{nameFile}")
	public Ret<String> spacePrivate(
			@PathVariable("nameBucket") String nameBucket,
			@PathVariable("nameFile") String nameFile
	) throws QiniuException
	{
		try
		{
			// 先不考虑私有m3u8的情况了
			var urlAuth = service.urlPrivate(nameBucket, nameFile, false);
			return Ret.success(urlAuth);
		}
		catch (Exception e)
		{
			return Ret.fail(e.getMessage());
		}
	}

	@SuppressWarnings("HttpUrlsUsage")
	@GetMapping("/space_public/{nameBucket}/{nameFile}")
	public Ret<String> spacePublic(
			@PathVariable("nameBucket") String nameBucket,
			@PathVariable("nameFile") String nameFile
	)
	{
		try
		{
			var url = service.urlPublic(nameBucket, nameFile);
			return Ret.success(url);
		}
		catch (Exception e)
		{
			return Ret.fail(e.getMessage());
		}
	}

}
