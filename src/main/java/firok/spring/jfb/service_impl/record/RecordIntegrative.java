package firok.spring.jfb.service_impl.record;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import firok.spring.jfb.bean.UploadRecordBean;
import firok.spring.jfb.bean.UploadSliceBean;
import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.record.IRecordIntegrative;
import firok.spring.jfb.service_impl.ContextKeys;
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
			throw new ExceptionIntegrative("创建记录时发生错误", e);
		}
	}
}
