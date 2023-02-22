package firok.spring.jfb.service_impl.storage;

import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.storage.IStorageIntegrative;
import firok.spring.jfb.constant.ContextKeys;

import java.io.File;
import java.util.Objects;

import static firok.spring.jfb.constant.ContextKeys.KEY_PROGRESS_NOW;
import static firok.spring.jfb.constant.ContextKeys.KEY_PROGRESS_TOTAL;
import static firok.topaz.general.Capacities.mb;
import static firok.topaz.general.Collections.mappingKeyValue;
import static firok.topaz.general.Collections.sumLong;
import static java.util.Arrays.asList;

class StorageTransferUtil
{
	public static void transfer(IStorageIntegrative service, WorkflowContext context) throws ExceptionIntegrative
	{
		File[] files;
		String nameBucket;
		synchronized (context.LOCK)
		{
			// gossip 那么文件列表长度为0的情况要不要做处理是个问题
			files = context.get(ContextKeys.KEY_FILES) instanceof File[] f ? f : null;
			nameBucket = String.valueOf(context.get(ContextKeys.KEY_NAME_BUCKET));
		}
		if(files == null) throw new ExceptionIntegrative("找不到上传列表");
		// 获取文件大小
		var mapFileSize = mappingKeyValue(asList(files), Objects::requireNonNull, File::length);
		long totalSize = sumLong(mapFileSize.values()); // 总大小

		synchronized (context.LOCK)
		{
			context.put(KEY_PROGRESS_NOW, 0);
			context.put(KEY_PROGRESS_TOTAL, (int) mb(totalSize));
		}

		long finishSize = 0;
		for (File file : files)
		{
			var size = mapFileSize.get(file);
			service.storeByFile(nameBucket, file);
			finishSize += size;
			synchronized (context.LOCK)
			{
				context.put(KEY_PROGRESS_NOW, (int) mb(finishSize));
			}
		}
	}
}
