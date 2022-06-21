# JFB - Java Flow Bytes

> 数据怎样来?  
> 数据怎样处理?  
> 数据怎样去?

一个后台动态工作流实现, 用于维护长时任务.  
目前扩展了部分文件数据持久化存储功能.

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
  * [DrawIO Diagrams - Microsoft Store](https://apps.microsoft.com/store/detail/drawio-diagrams/9MVVSZK43QQW)
  * [DrawIO Diagrams Online](https://app.diagrams.net/)

## 依赖

除了标准 SpringBoot 依赖,  
项目还依赖 [MVCI](https://github.com/351768593/MVCIntrospector) 工具和 [Topaz](https://github.com/351768593/Topaz) 库.

**后端基于 Java 17.**

前端编译打包需要使用 NodeJS.

## 配置运行环境

* 安装 ffmpeg 库和 ffprobe 库, 确保库可执行文件可以被 JFB 访问
* (可选) 安装并启用 MinIO
* (可选) 注册并启动七牛云
* (可选) 安装并启用 MySQL
* 安装 NodeJS
* 编译打包前端
* 编译打包后端

启动后直接访问 `http://{host}:{port}/` 即可.

关于编译打包前端:

```bash
cd web_uploader
# 编译打包前端资源
npm install
npm run build
# 这是一个 windows 批处理脚本 (script/package.bat)
# 用于将编译打包后的前端资源文件复制到后端静态资源目录
# 其它平台请手动复制相关文件 或编写复制脚本
npm run package
```
