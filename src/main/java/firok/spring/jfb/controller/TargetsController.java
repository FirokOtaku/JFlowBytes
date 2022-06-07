package firok.spring.jfb.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import firok.spring.jfb.bean.Ret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/target")
@ConditionalOnExpression("${app.controller-target.enable}")
@CrossOrigin(origins = "*")
public class TargetsController
{
	public record TargetInfo(String target, String bucket) { }
	/**
	 * 一个大存储空间内可能包含多个子桶
	 */
	public Map<String, Set<String>> mapTarget;
	@Value("${app.controller-target.targets}")
	public void setTargets(String raw) throws JsonProcessingException
	{
		var om = new ObjectMapper();
		// todo low 这里有可能出现json格式不对的情况 最好是做一下控制 但是现在没空写
		var arrayRaw = (ArrayNode) om.readTree(raw);
		var listTemp = new ArrayList<TargetInfo>(arrayRaw.size());
		for(var jsonRaw : arrayRaw)
		{
			var json = (ObjectNode) jsonRaw;
			var target = json.get("target").asText();
			var bucket = json.get("bucket").asText();
			var info = new TargetInfo(target, bucket);
			listTemp.add(info);
		}
		mapTarget = firok.topaz.Collections.mappingKeyMultiValueSet(listTemp, TargetInfo::target, TargetInfo::bucket);
	}

	/**
	 * 获取所有可用上传储存空间
	 */
	@GetMapping("/list_all_targets")
	public Ret<?> listAllTargets()
	{
		return Ret.success(mapTarget);
	}
}
