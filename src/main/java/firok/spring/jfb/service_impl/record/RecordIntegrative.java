package firok.spring.jfb.service_impl.record;

import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.jfb.bean.UploadRecordBean;
import firok.spring.jfb.bean.UploadSliceBean;
import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.record.IRecordIntegrative;
import firok.spring.jfb.service_impl.ContextKeys;
import firok.spring.jfb.util.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.io.File;
import java.util.Date;

import java.util.Map;

/**
 * 根据工作流上下文内容 在数据库里创建相应的文件记录
 */
@Service
@EnableTransactionManagement
@SuppressWarnings({"SpringJavaAutowiredMembersInspection", "SpringJavaInjectionPointsAutowiringInspection", "SpringJavaAutowiredFieldsWarningInspection"})
public class RecordIntegrative implements IRecordIntegrative, IWorkflowService
{
	public static final String SERVICE_NAME = ContextKeys.PREFIX + "record";

	@Override
	public String getWorkflowServiceOperation()
	{
		return SERVICE_NAME;
	}

	public static final String KEY_FILE_NAME = "file_name";
	public static final String KEY_FILE_SIZE = "file_size";

	@Override
	public Map<String, Class<?>> getWorkflowParamContext()
	{
		var ret = IWorkflowService.super.getWorkflowParamContext();
		ret.put(KEY_FILE_NAME, String.class);
		ret.put(KEY_FILE_SIZE, Number.class);
		ret.put(ContextKeys.KEY_NAME_BUCKET, String.class);
		ret.put(ContextKeys.KEY_TARGET, String.class);
		ret.put(ContextKeys.KEY_FILES, File[].class);
		return ret;
	}

	@Autowired
	IService<UploadRecordBean> serviceRecord;

	@Autowired
	IService<UploadSliceBean> serviceSlice;

	@Override
	@Transactional(rollbackFor = ExceptionIntegrative.class)
	public void operateWorkflow(WorkflowContext context) throws ExceptionIntegrative
	{
		var trans = TransactionAspectSupport.currentTransactionStatus();
		var sp = trans.createSavepoint();
		try
		{
			String fileName;
			long fileSize;
			String bucketName;
			String target;
			File[] files;
			synchronized (context.LOCK)
			{
				fileName = String.valueOf(context.get(KEY_FILE_NAME));
				fileSize = context.get(KEY_FILE_SIZE) instanceof Number num ? num.longValue() : 0;
				bucketName = String.valueOf(context.get(ContextKeys.KEY_NAME_BUCKET));
				target = String.valueOf(context.get(ContextKeys.KEY_TARGET));
				files = context.get(ContextKeys.KEY_FILES) instanceof File[] fs ? fs : new File[0];
			}
			Assertions.assertStrLenRange(fileName, 1, 255, "文件名长度不符合要求");
			Assertions.assertStrLenRange(bucketName, 1, 64, "桶名长度不符合要求");
			Assertions.assertStrLenRange(target, 1, 64, "持久化存储类型名长度不符合要求");
			Assertions.assertNumberRange(fileSize, 1, null, "原始文件大小不符合要求");

			var now = new Date();
			var record = new UploadRecordBean();
			record.setFileName(fileName);
			record.setFileSize(fileSize);
			record.setUploadTime(now);
			record.setBucketName(bucketName);
			record.setTarget(target);
			serviceRecord.save(record);
			var idRecord = record.getId();
			for(var file : files)
			{
				var slice = new UploadSliceBean();
				slice.setFileName(file.getName());
				slice.setFileSize(file.length());
				slice.setIdUploadRecord(idRecord);
				serviceSlice.save(slice);
			}
		}
		catch (Throwable e)
		{
			trans.rollbackToSavepoint(sp);
			throw new ExceptionIntegrative("创建上传记录时发生错误: "+e.getMessage(), e);
		}
	}
}
