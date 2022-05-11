package firok.spring.jfb.service_impl;

/**
 * 工作流上下文用到的一些键
 *
 * @implNote 这里的键都是工作流处理器间的共享键. 部分处理器有独有的临时键, 定义于处理器类内部.
 */
public interface ContextKeys
{
	/**
	 * 文件上传时的分片数量
	 */
	String KEY_COUNT_SLICE = "count_slice";

	/**
	 * 文件上传时的分片合并状态 : {@code boolean[] }
	 */
	String KEY_STATUS_SLICE = "status_slice";

	/**
	 * 在多个工作流处理器间传递文件列表 : {@code java.io.File[] }
	 */
	String KEY_FILES = "files";

	/**
	 * 要删除的文件列表 : {@code List<java.io.File> }
	 */
	String KEY_CLEAN_FILES = "clean_files";
}
