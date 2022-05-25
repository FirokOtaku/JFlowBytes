# JFlowBytes (Java Flow Bytes)

> 数据怎样来?  
> 数据怎样处理?  
> 数据怎样去?

一个后台动态工作流实现, 目前扩展了部分文件持久化存储功能.

* 普通文件上传
* 视频文件转码切片上传
* 持久化存储文件至 MinIO / 七牛云 OSS / 本地文件系统
* 转接文件提取请求至持久化储存
* 强扩展性 (如果你明白这一切代码结构的话) (也许)

部分文档

* [变更日志](doc/changelog.md)
* [设计细节](doc/design.md)
* [各工作流处理器描述](doc/workflow_integrative_description.md)
* ~~[最初的业务流程图](doc/task_file_upload.drawio)~~
  部分内容已经失效

## 依赖

除了标准 SpringBoot 依赖,  
项目还依赖 [MVCI](https://github.com/351768593/MVCIntrospector) 工具.
