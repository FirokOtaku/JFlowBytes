package firok.spring.jfb.controller;

import firok.spring.jfb.service_impl.storage.MinioStorageService;
import io.minio.GetObjectArgs;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@ConditionalOnBean(MinioStorageService.class)
@RestController
@RequestMapping("/api/storage/minio")
public class MinioStorageController
{
	@Autowired
	MinioStorageService service;

	@GetMapping("/read/{nameBucket}/{nameFile}")
	public void read(
			@PathVariable("nameFile") String nameFile,
			@PathVariable("nameBucket") String nameBucket,
			HttpServletResponse response
	) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException
	{
		response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(nameFile, StandardCharsets.UTF_8));

		var args = GetObjectArgs.builder()
				.bucket(nameBucket)
				.object(nameFile)
				.build();
		var obj = service.client.getObject(args);

		try(var os = response.getOutputStream())
		{
			obj.transferTo(os);
		}
	}
}
