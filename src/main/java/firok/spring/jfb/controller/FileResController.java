package firok.spring.jfb.controller;

import firok.spring.jfb.config.MinioConfig;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/fs")
@CrossOrigin(origins = "*")
public class FileResController
{
	@GetMapping("/get/{name}")
	public FileSystemResource get(
			@PathVariable("name") String name
	) {
		var file = new File("V:\\220322流媒体研究\\一些用到的视频片段\\test-04 复制流\\transcode\\" + name);
		return new FileSystemResource(file);
	}

	@Autowired
	MinioClient client;

	@Autowired
	MinioConfig config;

	@GetMapping("/minio/{name}")
	public void minio(
			@PathVariable("name") String name,
			HttpServletResponse response
	) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException
	{
		response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(name, StandardCharsets.UTF_8));

		var args = GetObjectArgs.builder()
				.bucket(config.nameBucket)
				.object(name)
				.build();
		var obj = client.getObject(args);

		try(var os = response.getOutputStream())
		{
			obj.transferTo(os);
		}
	}
}
