package firok.spring.jfb.controller;

import firok.spring.jfb.service_impl.storage.FileSystemStorageIntegrative;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.*;

import java.io.File;

/**
 * 本地文件系统持久化储存相关接口
 */
@ConditionalOnBean(FileSystemStorageIntegrative.class)
@RestController
@RequestMapping("/api/storage/filesystem")
@CrossOrigin(origins = "*")
public class FileSystemStorageController
{
	@Autowired
	FileSystemStorageIntegrative service;

	/**
	 * 获取指定文件数据, 返回文件二进制数据流
	 * */
	@GetMapping("/read/{nameBucket}/{nameFile}")
	@CrossOrigin(origins = "*")
	public FileSystemResource read(
			@PathVariable("nameFile") String nameFile,
			@PathVariable("nameBucket") String nameBucket
	)
	{
		var hash = service.mapperHash.mapHash(nameFile);
		var folderBucket = new File(service.folderStorage, nameBucket);
		var file = new File(folderBucket, hash.getHashString());
		return new FileSystemResource(file);
	}
}
