package firok.spring.jfb.service_impl.merge;

import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.merge.IFileMergeIntegrative;
import firok.spring.jfb.constant.ContextKeys;
import firok.topaz.resource.EnumerationMultiFileInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.SequenceInputStream;
import java.util.Map;

import static firok.spring.jfb.constant.ContextKeys.KEY_FILES;

@ConditionalOnExpression("${app.service-merge.enable:false}")
@Service
public class FileMergeIntegrative implements IFileMergeIntegrative, IWorkflowService
{
	public static final String SERVICE_NAME = ContextKeys.PREFIX + "file-merge";

	@Override
	public String getWorkflowServiceOperation()
	{
		return SERVICE_NAME;
	}

	@Value("${app.service-merge.filename-merge:file_merge.bin}")
	protected String filenameMerge;

	protected File mergeFileOf(WorkflowContext context)
	{
		return new File(context.folderWorkflowRoot, filenameMerge);
	}

	@Override
	public Map<String, Class<?>> getWorkflowParamContext()
	{
		var ret = IWorkflowService.super.getWorkflowParamContext();
		ret.put(KEY_FILES, File[].class);
		return ret;
	}

	@Override
	public void operateWorkflow(WorkflowContext context) throws ExceptionIntegrative
	{
		var files = (File[]) context.get(KEY_FILES); // 读取分片列表
		var fileMerge = mergeFileOf(context); // 创建临时合并文件

		try
		{
			mergeAll(files, fileMerge); // 合并文件
			// 合并成功的话 把分片文件列表放入待删除列表
			IWorkflowService.super.addFileToCleanList(context, files);
			// 更新上下文
			IWorkflowService.super.setFileList(context, fileMerge);
		}
		catch (Exception e)
		{
			// 合并失败的话 把合并文件放入待删除列表
			IWorkflowService.super.addFileToCleanList(context, fileMerge);

			// 通知上级
			throw new ExceptionIntegrative(e);
		}
	}

	@Override
	public void mergeAll(File[] files, File fileMerge) throws ExceptionIntegrative
	{
		var enumFiles = new EnumerationMultiFileInputStream(files);
		try(
				var ofs = new FileOutputStream(fileMerge);
				var ifs = new SequenceInputStream(enumFiles)
		)
		{
			ifs.transferTo(ofs);
		}
		catch (Exception e)
		{
			throw new ExceptionIntegrative(e);
		}
	}
}
