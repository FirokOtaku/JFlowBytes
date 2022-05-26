package firok.spring.jfb.controller;

import com.qiniu.common.QiniuException;
import firok.spring.jfb.bean.Ret;
import firok.spring.jfb.service_impl.storage.QiniuStorageIntegrative;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(description = "七牛云持久化储存相关接口")
@ConditionalOnBean(QiniuStorageIntegrative.class)
@RestController
@RequestMapping("/api/storage/qiniu")
public class QiniuStorageController
{
	@Autowired
	QiniuStorageIntegrative service;

	/**
	 * @deprecated 暂时没法测试这个
	 */
	@Deprecated
	@ApiOperation("获取一个私有储存空间内文件的访问地址")
	@GetMapping("/space_private/{nameBucket}/{nameFile}")
	public Ret<String> spacePrivate(
			@ApiParam("桶名称") @PathVariable("nameBucket") String nameBucket,
			@ApiParam("文件名称") @PathVariable("nameFile") String nameFile
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

	@ApiOperation("获取一个公共储存空间内文件的访问地址")
	@SuppressWarnings("HttpUrlsUsage")
	@GetMapping("/space_public/{nameBucket}/{nameFile}")
	public Ret<String> spacePublic(
			@ApiParam("桶名称") @PathVariable("nameBucket") String nameBucket,
			@ApiParam("文件名称") @PathVariable("nameFile") String nameFile
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
