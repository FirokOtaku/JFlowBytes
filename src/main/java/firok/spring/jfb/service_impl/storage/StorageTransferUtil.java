package firok.spring.jfb.service_impl.storage;

import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import firok.spring.jfb.service.storage.IStorageIntegrative;
import firok.spring.jfb.constant.ContextKeys;

import java.io.File;

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

		for (File file : files)
		{
			service.storeByFile(nameBucket, file);
		}
	}
}
