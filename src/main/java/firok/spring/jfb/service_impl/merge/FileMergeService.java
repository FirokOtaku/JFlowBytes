package firok.spring.jfb.service_impl.merge;

import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.merge.IFileMergeService;
import firok.spring.jfb.util.EnumerationMultiFileInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.SequenceInputStream;
import java.util.Map;

import static firok.spring.jfb.service_impl.ContextKeys.KEY_FILES;

@ConditionalOnExpression("${app.service-merge.enable:false}")
@Service
public class FileMergeService implements IFileMergeService, IWorkflowService
{
	@Override
	public String getWorkflowServiceOperation()
	{
		return "file_merge";
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

//	@Override
//	public void cleanWorkflow(WorkflowContext context, boolean isSuccess) throws ExceptionIntegrative
//	{
//		if(isSuccess) // 合并成功 删除分片文件
//		{
//			var files = (File[]) context.get(KEY_FILES);
//			for(var file : files)
//			{
//				// todo 这里也许可以开放一个配置项出来 允许退出时删除
//				file.delete();
//			}
//		}
//		else // 合并失败 删除合并后文件
//		{
//			var fileMerge = mergeFileOf(context);
//			fileMerge.delete();
//		}
//	}

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
