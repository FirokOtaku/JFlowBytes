package firok.spring.jfb.constant;

/**
 * 工作流上下文用到的一些键
 *
 * @implNote 这里的键都是工作流处理器间的共享键. 部分处理器有独有的临时键, 定义于处理器类内部.
 */
public interface ContextKeys
{
	String PREFIX = "jfb:";

	String CONTEXT_NOT_STARTED = PREFIX + "not_started";

	String CONTEXT_FINISHED = PREFIX + "finished";

	String CONTEXT_UNKNOWN = PREFIX + "unknown";

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

	/**
	 * 要上传到什么桶下: String
	 */
	String KEY_NAME_BUCKET = "name_bucket";

	/**
	 * 要上传到什么持久化储存: String
	 * */
	String KEY_TARGET = "name_target";

	/**
	 * 工作流某处理器执行总进度: int
	 */
	String KEY_PROGRESS_TOTAL = "progress_total";
	/**
	 * 工作流某处理器执行当前进度: int
	 */
	String KEY_PROGRESS_NOW = "progress_now";

	/**
	 * 工作流超时期限: Long
	 */
	String KEY_TIME_TIMEOUT_LIMIT = "time_timeout_limit";
}
