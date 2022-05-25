package firok.spring.jfb.controller;

import firok.spring.jfb.service_impl.storage.FileSystemStorageIntegrative;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@ConditionalOnBean(FileSystemStorageIntegrative.class)
@RestController
@RequestMapping("/api/storage/filesystem")
public class FileSystemStorageController
{
	@Autowired
	FileSystemStorageIntegrative service;

	@GetMapping("/read/{nameBucket}/{nameFile}")
	public FileSystemResource read(
			@PathVariable("nameFile") String nameFile,
			@PathVariable("nameBucket") String nameBucket
	)
	{
		var folderBucket = new File(service.folderStorage, nameBucket);
		var file = new File(folderBucket, nameFile);
		return new FileSystemResource(file);
	}
}
