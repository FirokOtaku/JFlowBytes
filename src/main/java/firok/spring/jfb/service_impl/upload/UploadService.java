package firok.spring.jfb.service_impl.upload;


import firok.spring.jfb.bean.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.upload.IUploadIntegrative;
import org.springframework.stereotype.Service;

import javax.annotation.processing.Filer;
import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;

/**
 * 工作流处理器 - 文件上传阶段 <br>
 *
 * 这个处理器一般用作工作流起始点. <br>
 * 处理器实际不进行任何操作, 仅对目录状态作检查. <br>
 * 如果用户已经上传了所有分片, 则此阶段完成.
 *
 * 出参:
 * files : java.io.File[] - 用户上传的文件分片列表
 *
 * @author Firok
 */
@Service
public class UploadService implements IUploadIntegrative, IWorkflowService
{

	@Override
	public String getWorkflowServiceOperation()
	{
		return "upload";
	}

	@Override
	public Map<String, Class<?>> getWorkflowParamContext()
	{
		var params = IWorkflowService.super.getWorkflowParamContext(); // 至于为什么要这么写 因为开心
		params.put("countSlice", int.class); // 上传文件分片量
		return params;
	}

	@Override
	public void operateWorkflow(WorkflowContext context) throws ExceptionIntegrative
	{
		int countSlice = (Integer) context.get("countSlice");
		// 根据分片数量创建一个文件列表和状态列表
		var files = new java.io.File[countSlice];
		for(var stepSlice = 0; stepSlice < countSlice; stepSlice++)
		{
			files[stepSlice] = new File(null, "slice_" + stepSlice + ".bin");
		}

		var status = (boolean[]) context.get("status_slice");
		if(status == null || status.length != countSlice)
			throw new ExceptionIntegrative("上传文件分片状态列表长度不正确");

		// 阻塞当前线程 持续检查上下文状态
		CHECK_SLICE_UPLOAD_STATUS: while(true)
		{
			boolean pass = true;
			for(var stepSlice = 0; stepSlice < countSlice; stepSlice++)
			{
				if(!status[stepSlice])
				{
					pass = false;
					break;
				}
			}

			if(pass)
			{
				for(var stepSlice = 0; stepSlice < countSlice; stepSlice++)
				{
					var flagSlice = "upload_" + stepSlice;
					context.remove(flagSlice);
				}
				break CHECK_SLICE_UPLOAD_STATUS;
			}

			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				break;
			}
		}

		context.remove("status_slice"); // 状态列表用不到了

		context.put("files", files);
	}

	@Override
	public void cleanWorkflow(WorkflowContext context, boolean isSuccess) throws ExceptionIntegrative
	{
		// 上传成功 删除所有分片缓存文件
		// 上传失败 删除合并失败的文件
	}
}
