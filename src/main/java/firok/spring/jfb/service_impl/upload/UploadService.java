package firok.spring.jfb.service_impl.upload;


import firok.spring.jfb.bean.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.upload.IUploadIntegrative;
import firok.spring.jfb.service_impl.ContextKeys;
import org.springframework.stereotype.Service;

import javax.annotation.processing.Filer;
import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;

import static firok.spring.jfb.service_impl.ContextKeys.KEY_COUNT_SLICE;
import static firok.spring.jfb.service_impl.ContextKeys.KEY_STATUS_SLICE;

/**
 * 工作流处理器 - 文件上传阶段 <br>
 *
 * 这个处理器一般用作工作流起始点. <br>
 * 处理器实际不进行任何操作, 仅对目录状态作检查. <br>
 * 如果用户已经上传了所有分片, 则此阶段完成.
 *
 * 入参:
 * count_slice : 分片数量
 * status_slice : 分片上传状态
 *
 * 临时变量:
 *
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
		params.put(KEY_COUNT_SLICE, int.class); // 上传文件分片量
		return params;
	}

	@Override
	public boolean hasAllSliceUploaded(WorkflowContext context)
	{
		var status = (boolean[]) context.get(KEY_STATUS_SLICE); // fixme low 其实这里有可能出现类型转换错误 暂时不管了
		synchronized (context.LOCK)
		{
			for(var statusSlice : status) // fixme low 这里也有可能空指针错误
			{
				// 如果有任何一个分片没上传成功
				if(!statusSlice)
				{
					return false;
				}
			}
			return true;
		}
	}

	@SuppressWarnings("BusyWait")
	@Override
	public void operateWorkflow(WorkflowContext context) throws ExceptionIntegrative
	{
		int countSlice = (Integer) context.get(KEY_COUNT_SLICE);
		var status = (boolean[]) context.get(KEY_STATUS_SLICE);
		if(status == null || status.length != countSlice)
			throw new ExceptionIntegrative("上传文件分片状态列表长度不正确");

		// 阻塞当前线程 持续检查上下文状态
		while (!hasAllSliceUploaded(context))
		{
			try // 没有完成上传 继续阻塞
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				throw new ExceptionIntegrative("用户取消上传或上传监控线程中断", e);
			}
		}
	}

	@Override
	public void cleanWorkflow(WorkflowContext context, boolean isSuccess) throws ExceptionIntegrative
	{
		if(isSuccess)
		{
			// 分片数量和状态列表用不到了
			context.remove(KEY_COUNT_SLICE);
			context.remove(KEY_STATUS_SLICE);
		}
		// 这个任务失败的情况只能是发生了超时 线程被打断了
		// 这种情况下把所有已经上传的分片删掉就行
		else
		{
			int countSlice = (Integer) context.get(KEY_COUNT_SLICE);

		}
	}
}
