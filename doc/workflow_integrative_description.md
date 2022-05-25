# 各工作流处理器描述

* 如果某个参数只在入参列表, 但不再出参列表:
  此参数前后意义 **不发生变化**
* 如果某个参数既在入参列表, 也在出参列表:
  此参数前后意义 **发生变化**
* 如果某个参数只存在于临时变量列表:
  此参数会处理完成后 **清除**
* 如果某个参数不在任何列表:
  此参数运行过程中 **不会受到任何影响**

## 分片上传 `jfb:upload`

此处理器会持续等待,  
用户将所有分片上传完成后会自动结束.

入参:

* `count_slice`: int
  上传文件分片数量. 正整数.

临时变量:

* `status_slice`: boolean[]
  分片上传状态

出参:

* `files`: java.io.File[]
  上传的分片文件列表, 长度大于等于1.

## 合并上传分片 `jfb:file-merge`

此处理器会将指定的文件合并为单一文件.

入参:

* `files`: java.io.File[]
  需要合并的文件列表.

出参:

* `files`: java.io.File[]
  合并后的文件. 数组长度为 1.

## 调用 FFMPEG 将文件转码切片为 m3u8 `jfb:ffmpeg-transcode-m3u8`

此处理器会将指定文件使用 FFMPEG 切片转码为 m3u8 格式.

入参:

* `files`: java.io.File[]
  需要转码的文件. 数组长度为 1.

出参:

* `files`: java.io.File[]
  以 m3u8 格式切片转码之后的文件列表.
* `folder_m3u8`: java.io.File
  储存转码后所有文件的目录.
* `file_m3u8`: java.io.File
  转码后的 m3u8 索引文件.

## 持久化存储至文件系统 `jfb:filesystem-storage`

此处理器会将指定文件列表存于本地文件系统指定目录.

入参:

* `files`: java.io.File[]
  需要持久化存储的文件列表.
* `name_bucket`: String
  需要存入的子目录名称.

## 持久化存储至 MinIO `jfb:minio-storage`

此处理器会将指定文件列表存于 MinIO.

入参:

* `files`: java.io.File[]
  需要持久化存储的文件列表.
* `name_bucket`: String
  需要存入的桶名称.

## 持久化存储至七牛云 OSS `jfb:qiniu-storage`

此处理器会将指定文件列表存于七牛云 OSS.

入参:

* `files`: java.io.File[]
  需要持久化存储的文件列表.
* `name_bucket`: String
  需要存入的桶名称.

## 上传记录器 `jfb:record`

此处理器会在数据库创建一次上传记录,  
包含任务信息和此任务相关的文件列表.

入参:

* `files`: java.io.File[]:
  相关文件列表.
* `file_name`: String
  原始文件名.
* `file_size`: int
  原始文件大小.
* `name_bucket`: String
  存入的桶名称.
* `name_target`: String
  持久化存储的类型名称. (`minio`, `qiniu`, `filesystem`)
