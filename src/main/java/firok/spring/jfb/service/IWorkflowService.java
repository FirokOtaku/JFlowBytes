package firok.spring.jfb.service;

import firok.spring.jfb.constant.ContextKeys;
import firok.spring.jfb.flow.WorkflowContext;
import org.apache.tomcat.util.http.fileupload.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import static firok.spring.jfb.constant.ContextKeys.*;

/**
 * 标明某个 service 支持工作流化调用
 */
public interface IWorkflowService
{
	/**
	 * @return 此工作流的操作类型名称, 需全局唯一, 否则在初始化工作流管理器时会报错
	 * @implNote 不能返回 null
	 */
	String getWorkflowServiceOperation();

	/**
	 * @return 此工作流想要正确开始操作时, 所需上下文内包含的参数
	 * @implNote 不能返回 null
	 */
	default Map<String, Class<?>> getWorkflowParamContext()
	{
		return new HashMap<>();
	}

	/**
	 * 判断某个上下文是否允许开始此操作
	 * @param context 工作流上下文
	 * @return 工作流上下文是否允许开始此操作
	 * @implNote 此方法不应对 @{@code #getWorkflowParamContext() } 中数据做出修改
	 */
	default boolean isWorkflowContextSuitable(WorkflowContext context)
	{
		var mapParam = getWorkflowParamContext();
		for(var entryParam : mapParam.entrySet())
		{
			var nameParam = entryParam.getKey();
			var typeParam = entryParam.getValue();
			if(!context.hasParamOfType(nameParam, typeParam))
			{
				context.log(Level.FINE, "工作流上下文参数缺失或类型错误: " + nameParam);
				return false;
			}
		}
		return true;
	}

	/**
	 * 开始操作工作流上下文
	 * @param context 工作流上下文
	 * @throws ExceptionIntegrative 发生任何错误时都可抛出, 此时工作流管理器会视为操作失败
	 * @implNote 遇到错误不需要主动进行清理, 只需要抛出异常通知工作流管理器处理过程发生错误, 由管理器调用 {@code #cleanWorkflow } 方法进行清理
	 */
	void operateWorkflow(WorkflowContext context) throws ExceptionIntegrative;

	/**
	 * 清理工作流上下文. 无论此操作成功完成还是失败完成, 都会立刻调用此方法
	 * @param context 工作流上下文
	 * @param isSuccess 本次清理操作是否发生于操作成功后
	 * @throws ExceptionIntegrative 发生任何错误时都可抛出. 根据配置不同, 清理操作发生错误可能会导致任务失败
	 * @implNote 由于部分处理器的具体实现, 导致任务处理阶段完成或失败后, 必须先执行清理过程, 再次运行才不会出现问题
	 */
	@SuppressWarnings("unchecked")
	default void cleanWorkflow(WorkflowContext context, boolean isSuccess) throws ExceptionIntegrative
	{
		List<File> listFileClean = context.get(KEY_CLEAN_FILES) instanceof List list ? list : null;
		if(listFileClean == null)
		{
			listFileClean = new ArrayList<>();
			context.put(KEY_CLEAN_FILES, listFileClean);
		}

		// 清空文件
		Iterator<File> iterator = listFileClean.iterator();
		while (iterator.hasNext())
		{
			File objFile = iterator.next();
			try
			{
				FileUtils.forceDelete(objFile);
				iterator.remove(); // 正常删除文件的话可以移除枚举器
			}
			catch (FileNotFoundException | NullPointerException e)
			{
				// 文件已经没了 或者列表里出现了null元素
				// 这种情况不算异常
				// gossip 说实话这列表出现null元素是挺异常的 但是这块的逻辑不太在意这个
				iterator.remove();
			}
			catch (IOException e) // 其它异常才有问题
			{
				throw new ExceptionIntegrative(e);
			}
		}
	}

	/**
	 * 把指定文件置入工作流上下文
	 * @param context 工作流上下文
	 * @param files 文件列表. 为null则清空变量
	 */
	default void setFileList(WorkflowContext context, File... files)
	{
		if(files == null) context.remove(KEY_FILES);
		else context.put(KEY_FILES, files);
	}

	/**
	 * 把指定文件置入工作流上下文的待清理列表
	 * @param context 工作流上下文
	 * @param files 文件列表
	 */
	@SuppressWarnings({"unchecked"})
	default void addFileToCleanList(WorkflowContext context, File... files)
	{
		var objFiles = context.get(KEY_CLEAN_FILES);
		List<File> list;
		if (objFiles instanceof List)
			list = (List<File>) objFiles;
		else
		{
			list = new ArrayList<>();
			context.put(KEY_CLEAN_FILES, list);
		}

		if(files != null)
			Collections.addAll(list, files);
	}
}
