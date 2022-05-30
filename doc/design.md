# 设计细节

## 工作流和处理器类

* **工作流**
  由调用者创建的一整个处理流程, 包含若干处理步骤.
* **工作流处理器**
  工作流每个处理步骤对应代码中一个工作流处理器.

* 这个系统的 *工作流* **不是** Flowable 框架的工作流,  
  工作流实例仅有运行时数据储存于内存, 没有任何持久化操作.  
  也就是说程序执行中退出的话 **没法恢复现场**.
* 每个 *工作流处理器* 都对应一个长时 (或非长时) 任务.
* 每个 *工作流处理器*
* 大部分 *工作流处理类* 仅基于工作流上下文内的变量执行操作,  
  类似函数式编程,  执行过程 **不依赖和影响系统其它部分状态**, **不依赖和影响其它工作流上下文**, **不需也不能被外界控制**.
* *工作流* 从创建到执行结束, 按创建时确定的步骤 **顺序执行**,  
  外界只能获取工作流当前的状态和日志,  
  或者直接停止某个工作流.

### 文件上传

文件上传过程比较特殊, 因为这个流程需要跟前端产生数据交互.

相关处理器仅循环检测工作流上下文状态.  
没有上传完成前都会堵塞线程, 直到上传完成后才停止处理过程.

## 如何新增工作流处理器种类

所有工作流处理器类首先需要实现 `firok.spring.jfb.service.IWorkflowService` 接口.  
实现接口需要注意的内容在接口类的 Javadoc 里有详细说明.

之后, 只要在相关类增加 `@Service` 或其它注解,
使得该类能够加载到 Spring 上下文即可.

扫描过程实现在 `firok.spring.jfb.controller.FlowController.scanWorkflowServices` 方法.

## Spring config 类

因为有多个 "一 config 对应一 service" 的情况,  
想了想没必要为相关配置项专门创建一个 config 类了,  
直接把配置项用 `@Value` 注入到 service 实例里,  
把 service 实例当作 config 实例用也不是不行.