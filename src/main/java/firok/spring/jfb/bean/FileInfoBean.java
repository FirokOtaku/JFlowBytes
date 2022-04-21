package firok.spring.jfb.bean;

import firok.spring.mvci.MVCIntrospective;
import lombok.Data;

/**
 * 文件数据实体
 *
 * @author firok
 */
@Data
@MVCIntrospective
public class FileInfoBean extends BaseBean
{
	/**
	 * 文件名
	 */
	private String filename;

	/**
	 * 文件大小
	 */
	private Long filesize;

	/**
	 * 文件类型
	 * 可选值: single, slice_group
	 */
	private String filetype;

	/**
	 * 文件分片数
	 * 如果文件类型为 single, 此字段无意义
	 */
	private Integer countSlice;

	/**
	 * 文件分片大小
	 * 如果文件类型为 single, 此字段无意义
	 */
	private Integer sizeSlice;
}
