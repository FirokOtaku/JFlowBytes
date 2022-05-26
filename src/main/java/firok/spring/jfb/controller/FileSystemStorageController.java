package firok.spring.jfb.controller;

import firok.spring.jfb.service_impl.storage.FileSystemStorageIntegrative;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@Api(description = "本地文件系统持久化储存相关接口")
@ConditionalOnBean(FileSystemStorageIntegrative.class)
@RestController
@RequestMapping("/api/storage/filesystem")
public class FileSystemStorageController
{
	@Autowired
	FileSystemStorageIntegrative service;

	@ApiOperation("获取指定文件数据, 返回文件二进制数据流")
	@GetMapping("/read/{nameBucket}/{nameFile}")
	public FileSystemResource read(
			@ApiParam("桶名称") @PathVariable("nameFile") String nameFile,
			@ApiParam("文件名称") @PathVariable("nameBucket") String nameBucket
	)
	{
		var folderBucket = new File(service.folderStorage, nameBucket);
		var file = new File(folderBucket, nameFile);
		return new FileSystemResource(file);
	}
}
