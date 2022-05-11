package firok.spring.jfb.service.upload;

import firok.spring.jfb.bean.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;

public interface IUploadIntegrative
{
	/**
	 * @param context 工作流上下文
	 * @return 所有分片是否上传完成
	 * @throws ExceptionIntegrative 发生任何错误时抛出
	 */
	boolean hasAllSliceUploaded(WorkflowContext context) throws ExceptionIntegrative;
}
