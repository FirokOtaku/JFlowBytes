package firok.spring.jfb.bean;

import lombok.Data;

/**
 * 文件数据实体
 *
 * @author firok
 */
@Data
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

	/**
	 * 文件上传状态
	 * 可选值: 0 - 初始化, 1 - 正在上传, 2 - 上传完成, 3 - 上传失败或取消
	 */
	private Integer uploadStatus;
}
