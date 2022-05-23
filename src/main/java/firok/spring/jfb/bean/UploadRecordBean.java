package firok.spring.jfb.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import firok.spring.mvci.MVCIntrospective;
import lombok.Data;

import java.util.Date;

/**
 * 一条上传记录
 */
@Data
@MVCIntrospective
@TableName("d_upload_record")
public class UploadRecordBean extends BaseBean
{
	/**
	 * 文件原始名称
	 */
	String fileName;
	/**
	 * 文件原始大小
	 */
	Long fileSize;

	/**
	 * 文件上传时间
	 */
	Date uploadTime;

	/**
	 * 文件上传到的永久化储存空间名称
	 */
	String target;

	/**
	 * 上传的桶名称
	 *
	 */
	String bucketName;
}
