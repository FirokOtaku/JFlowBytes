package firok.spring.jfb.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.mapper.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.jfb.bean.Ret;
import firok.spring.jfb.bean.UploadRecordBean;
import firok.spring.jfb.service_impl.record.RecordIntegrative;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

/**
 * 上传记录相关接口
 * */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RestController
@RequestMapping("/api/record")
@ConditionalOnBean(RecordIntegrative.class)
@CrossOrigin(origins = "*")
public class RecordController
{
	@Autowired
	IService<UploadRecordBean> serviceRecord;

	@Autowired
	BaseMapper<UploadRecordBean> mapperRecord;

	/**
	 * 获取上传记录用参数
	 * */
	public record ListRecordParams(String filterTarget, String filterBucketName, String filterFileName){};
	/**
	 * 获取上传记录
	 */
	@PostMapping("/list_records")
	public Ret<?> listRecords(
			@RequestBody(required = false) ListRecordParams filter,
			@RequestParam(value = "pageIndex", defaultValue = "1") int pageIndex,
			@RequestParam(value = "pageSize", defaultValue = "10") int pageSize
	)
	{
		if(pageIndex < 1) pageIndex = 1;
		if(pageSize < 1) pageSize = 1;
		if (pageSize > 50) pageSize = 50;
		var filterFileName = filter != null ? filter.filterFileName() : null;
		var filterTarget = filter != null ? filter.filterTarget() : null;
		var filterBucketName = filter != null ? filter.filterBucketName() : null;
		var page = mapperRecord.selectPage(new Page<>(pageIndex, pageSize), new QueryWrapper<UploadRecordBean>().lambda()
				.like(filterFileName != null, UploadRecordBean::getFileName, '%' + filterFileName + '%')
				.like(filterBucketName != null, UploadRecordBean::getBucketName, '%' + filterBucketName + '%')
				.eq(filterTarget != null, UploadRecordBean::getTarget, filterTarget)
		);
		return Ret.success(page);
	}

	@DeleteMapping("/delete_record")
	public Ret<?> deleteRecord(
			@RequestParam("id_record") String id
	)
	{
		;
		return Ret.success();
	}
}
