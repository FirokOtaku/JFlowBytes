package firok.spring.jfb.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.mapper.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.jfb.bean.Ret;
import firok.spring.jfb.bean.UploadRecordBean;
import firok.spring.jfb.bean.UploadSliceBean;
import firok.spring.jfb.flow.WorkflowServices;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.storage.IStorageIntegrative;
import firok.spring.jfb.service_impl.record.RecordIntegrative;
import firok.spring.jfb.util.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
	IService<UploadSliceBean> serviceSlice;

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
		var page = serviceRecord.page(new Page<>(pageIndex, pageSize), new QueryWrapper<UploadRecordBean>().lambda()
				.like(filterFileName != null, UploadRecordBean::getFileName, '%' + filterFileName + '%')
				.like(filterBucketName != null, UploadRecordBean::getBucketName, '%' + filterBucketName + '%')
				.eq(filterTarget != null, UploadRecordBean::getTarget, filterTarget)
				// 最新上传的文件排在前面
				.orderBy(true, false, UploadRecordBean::getUploadTime)
		);
		return Ret.success(page);
	}

	@Autowired
	WorkflowServices services;

	/**
	 * 删除上传记录, 这会尝试把存储空间中的文件一并删除
	 */
	@Transactional(rollbackFor = Exception.class)
	@DeleteMapping("/delete_record")
	public Ret<?> deleteRecord(
			@RequestParam("id_record") String id
	) throws ExceptionIntegrative
	{
		// 先把数据库里的上传记录删掉
		// 然后把对应储存里的文件删掉
		// 因为我们暂时默许储存空间容量无限
		// 所以删除文件时发生的错误不重要
		// todo low 以后需要新增一个定期清理储存空间里无效文件的定时器 等有空再说吧

		var qw = new QueryWrapper<UploadSliceBean>().lambda()
				.eq(UploadSliceBean::getIdUploadRecord, id);

		String target, bucket;
		String[] arrFileNames;
		try // 查询数据库里有没有指定记录
		{
			var record = this.serviceRecord.getById(id);
			target = record.getTarget();
			bucket = record.getBucketName();
			var slices = this.serviceSlice.list(qw);
			var setFileName = slices.stream()
					.map(UploadSliceBean::getFileName)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
			arrFileNames = setFileName.isEmpty() ? null : setFileName.toArray(new String[0]);
		}
		catch (Exception e)
		{
			return Ret.fail("找不到指定上传记录");
		}

		try
		{
			// 删除数据库上传记录
			this.serviceRecord.removeById(id);
			// 删除数据库切片记录
			this.serviceSlice.remove(qw);
		}
		catch(Exception e)
		{
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			return Ret.fail("删除数据库记录时发生错误: " + e.getMessage());
		}

		try // 删除储存空间文件
		{
			// 没有需要删的文件 直接返回成功
			if(arrFileNames == null) return Ret.success("删除成功");

			// 开始寻找相关工作流处理器
			var list = services.getServicesOf(IStorageIntegrative.class);
			for(var service : list)
			{
				if(Objects.equals(service.getStorageTargetName(), target))
				{
					// 调用处理器删除
					service.delete(bucket, arrFileNames);

					return Ret.success().setMsg("删除成功");
				}
			}
			throw new ExceptionIntegrative("不存在指定工作流处理器, 或相关处理器未启用: " + target);
		}
		catch (Exception e)
		{
			return Ret.success().setMsg("删除储存空间文件时发生错误, 这通常不会影响系统其它部分: " + e.getMessage());
		}
	}

	/**
	 * 重命名上传记录参数
	 */
	public record RenameRecordParam(String id, String name) { }

	/**
	 * 重命名上传记录
	 */
	@Transactional
	@PatchMapping("/rename_record")
	public Ret<?> renameRecord(
			@RequestBody RenameRecordParam params
	)
	{
		try
		{
			Assertions.assertStrLenRange(params.name(), 1, 255, "上传记录名称长度必须在1~255之间");

			var record = this.serviceRecord.getById(params.id());
			if(record == null) return Ret.fail("找不到指定上传记录");

			record.setFileName(params.name());
			this.serviceRecord.updateById(record);

			return Ret.success();
		}
		catch (Exception e)
		{
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			return Ret.fail("重命名上传记录文件名失败: " + e.getMessage());
		}
	}
}
