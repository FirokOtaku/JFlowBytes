package firok.spring.jfb.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import firok.spring.mvci.MVCIntrospective;
import lombok.Data;

/**
 * 具体持久化储存的文件信息
 */
@Data
@MVCIntrospective
@TableName("d_upload_slice")
public class UploadSliceBean extends BaseBean
{
	/**
	 * 上传记录id
	 */
	Long idUploadRecord;

	/**
	 * 文件名
	 */
	String fileName;

	/**
	 * 文件大小
	 */
	Long fileSize;
}
