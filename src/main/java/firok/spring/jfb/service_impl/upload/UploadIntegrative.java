package firok.spring.jfb.service_impl.upload;


import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.IWorkflowService;
import firok.spring.jfb.service.upload.IUploadIntegrative;
import firok.spring.jfb.service_impl.ContextKeys;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static firok.spring.jfb.service_impl.ContextKeys.*;

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
public class UploadIntegrative implements IUploadIntegrative, IWorkflowService
{
	public static final String SERVICE_NAME = ContextKeys.PREFIX + "upload";

	@Override
	public String getWorkflowServiceOperation()
	{
		return SERVICE_NAME;
	}

	@Override
	public Map<String, Class<?>> getWorkflowParamContext()
	{
		var params = IWorkflowService.super.getWorkflowParamContext(); // 至于为什么要这么写 因为开心
		params.put(KEY_COUNT_SLICE, int.class); // 上传文件分片量
		return params;
	}

	@Override
	public boolean[] uploadSlice(WorkflowContext context, int sliceIndex, MultipartFile fileSlice) throws ExceptionIntegrative
	{
		synchronized (context.LOCK)
		{
			// 检查指定分片是否上传完成
			boolean[] statusSlice = context.get(KEY_STATUS_SLICE) instanceof boolean[] status ? status : null;
			Integer countSlice = context.get(KEY_COUNT_SLICE) instanceof Integer count ? count : null;
			File[] listFiles = context.get(ContextKeys.KEY_FILES) instanceof File[] files ? files : null;

			if(statusSlice == null || countSlice == null || listFiles == null || listFiles.length != statusSlice .length || statusSlice.length != countSlice)
				throw new ExceptionIntegrative("工作流上下文参数错误");
			if(sliceIndex < 0 || sliceIndex >= countSlice)
				throw new ExceptionIntegrative("分片索引超出范围");
			if(statusSlice[sliceIndex])
				throw new ExceptionIntegrative("指定分片已经完成上传, 不能重复上传");

			var cacheSlice = new File(context.folderWorkflowRoot, "slice_" + sliceIndex);
			listFiles[sliceIndex] = cacheSlice;
			if(cacheSlice.exists())
				cacheSlice.delete();
			cacheSlice.getParentFile().mkdirs();

			try(
				var ifs = fileSlice.getInputStream();
				var ofs = new FileOutputStream(cacheSlice);
			)
			{
				ifs.transferTo(ofs);
				statusSlice[sliceIndex] = true;
				return Arrays.copyOf(statusSlice, statusSlice.length);
			}
			catch (IOException e)
			{
				throw new ExceptionIntegrative("上传分片时发生错误" + e.getMessage(), e);
			}
		}
	}

	@Override
	public boolean hasAllSliceUploaded(WorkflowContext context)
	{
		synchronized (context.LOCK)
		{
			// fixme 这个地方的写法可能需要改一下 如果没找到上下文里的status列表就报错
			var status = context.get(KEY_STATUS_SLICE) instanceof boolean[] arr ? arr : new boolean[] { false };
			for(var statusSlice : status)
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

	@SuppressWarnings({"BusyWait", "unchecked"})
	@Override
	public void operateWorkflow(WorkflowContext context) throws ExceptionIntegrative
	{
		int countSlice;
		synchronized (context.LOCK)
		{
			countSlice = context.get(KEY_COUNT_SLICE) instanceof Integer count ? count : -1;
		}
		if(countSlice <= 0)
			throw new ExceptionIntegrative("上传文件分片参数不正确");
		var status = new boolean[countSlice];
		var files = new File[countSlice];
		for(int step = 0; step < countSlice; step++)
			files[step] = new File(context.folderWorkflowRoot, "slice_" + step + ".bin");

		synchronized (context.LOCK)
		{
			context.put(KEY_STATUS_SLICE, status);
			context.put(KEY_FILES, files);
		}

		Exception exception = null;

		// 阻塞当前线程 持续检查上下文状态
		while (!hasAllSliceUploaded(context))
		{
			try // 没有完成上传 继续阻塞
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				exception = e;
				break;
			}
		}

		if(exception == null)
		{
			// 分片数量和状态列表用不到了
			context.remove(KEY_COUNT_SLICE);
			context.remove(KEY_STATUS_SLICE);
		}
		// 这个任务失败的情况只能是发生了超时 线程被打断了
		// 这种情况下把所有已经上传的分片删掉就行
		else
		{
			IWorkflowService.super.addFileToCleanList(
					context,
					files
			);
			throw new ExceptionIntegrative("用户取消上传或上传监控线程中断", exception);
		}
	}
}
