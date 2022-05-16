package firok.spring.jfb.service.upload;

import firok.spring.jfb.flow.WorkflowContext;
import firok.spring.jfb.service.ExceptionIntegrative;
import org.springframework.web.multipart.MultipartFile;

public interface IUploadIntegrative
{
	/**
	 * 向一个工作流上传一个文件分片
	 * @param context 工作流上下文
	 * @param sliceIndex 分片索引
	 * @param fileSlice 分片文件
	 * @return 分片上传状态
	 * @throws ExceptionIntegrative 发生任何错误时抛出
	 */
	boolean[] uploadSlice(WorkflowContext context, int sliceIndex, MultipartFile fileSlice) throws ExceptionIntegrative;

	/**
	 * @param context 工作流上下文
	 * @return 所有分片是否上传完成
	 * @throws ExceptionIntegrative 发生任何错误时抛出
	 */
	boolean hasAllSliceUploaded(WorkflowContext context) throws ExceptionIntegrative;
}
