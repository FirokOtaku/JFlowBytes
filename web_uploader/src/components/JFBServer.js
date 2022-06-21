import axios from "axios";
import { notEmpty } from './Strings';

export const urlVideoServer = process.env.VUE_APP_JFB_BASE_URL;

export async function request(config = {})
{
  return new Promise((resolve, reject) => {
    axios(config)
      .then(res => {
        const { data } = res;
        if(data.success) resolve(data.data);
        else reject(data.msg);
      })
      .catch(err => {
        reject(err);
      });
  });
}

/**
 * 创建一个按照指定切片大小返回文件切片的迭代器
 * @param file {Blob} 文件
 * @param sliceSize {number} 切片大小. 不传默认为 5 MB
 * @returns {Iterator<Blob>} 切片迭代器
 * @example
 * for(const slice of slicesOf(file)) { ... }
 * */
export function* slicesOf(file, sliceSize = 1024 * 1024 * 5)
{
  const fileSize = file.size;
  const sliceCount = Math.ceil(fileSize / sliceSize);
  for(let sliceIndex = 0; sliceIndex < sliceCount; sliceIndex++)
  {
    // 计算切片的起始位置
    const posStart = sliceIndex * sliceSize;
    const posStop = Math.min(fileSize, posStart + sliceSize);
    // 进行一个片的切
    const filePart = file.slice(posStart, posStop);

    yield filePart;
  }
}

/**
 * 适用于 JFB 0.1.19 API
 * */
export class JFBServer
{
  async createWorkflow({ listOperation, mapContextInitParam })
  {
    return await request({
      method: 'post',
      url: urlVideoServer + '/api/workflow/create_workflow',
      data: {
        listOperation,
        mapContextInitParam,
      },
    });
  }

  async deleteWorkflow(idWorkflow)
  {
    return await request({
      url: urlVideoServer + '/api/workflow/delete_workflow',
      method: 'delete',
      params: { idWorkflow, },
    });
  }

  async listCurrentWorkflows(lenLog,...listWorkflowId)
  {
    if(lenLog == null) lenLog = 3;
    return await request({
      url: urlVideoServer + '/api/workflow/list_current_workflow',
      method: 'post',
      data: { listWorkflowId, lenLog },
    });
  }

  async serviceUploadSlice({ idWorkflow, indexSlice, slice}, callbackWhenProgress)
  {
    const form = new FormData();
    form.append('file', slice);
    return await request({
      url: urlVideoServer + '/api/workflow/service_upload_slice',
      method: 'post',
      params: { idWorkflow, indexSlice, },
      data: form,
      onUploadProgress: callbackWhenProgress,
    });
  }

  async listWorkflowService()
  {
    return await request({
      url: urlVideoServer + '/api/workflow/list_workflow_service',
      method: 'get',
    });
  }

  /**
   * 获取视频列表
   * */
  async listRecords({
    filterTarget,
    filterBucketName,
    filterFileName,
    pageIndex,
    pageSize,
  }) {
     filterTarget = notEmpty(filterTarget);
     filterBucketName = notEmpty(filterBucketName);
     filterFileName = notEmpty(filterFileName);
     if(pageIndex == null) pageIndex = 1;
     if(pageSize == null) pageSize = 12;

     return await request({
       url: urlVideoServer + '/api/record/list_records/',
       method: 'post',
       params: {
         pageIndex,
         pageSize,
       },
       data: {
         filterTarget,
         filterBucketName,
         filterFileName,
       },
     })
  }

  /**
   * 删除上传记录
   * */
  async deleteRecord(idRecord)
  {
    return await request({
      url: urlVideoServer + '/api/record/delete_record',
      method: 'delete',
      params: { id_record: idRecord },
    });
  }

  /**
   * 重命名上传记录
   * */
  async renameRecord(id, name)
  {
    return await request({
      url: urlVideoServer + '/api/record/rename_record',
      method: 'patch',
      data: { id, name },
    });
  }

  /**
   * 获取某个文件的访问路径
   * MinIO 和本地文件系统可以直接返回
   * 七牛云的要过一遍后台接口
   * */
  async urlFile({ target, bucketName, fileName, })
  {
    return new Promise((resolve, reject) => {
      switch(target)
      {
        case 'minio': case 'filesystem':
          resolve(urlVideoServer + `/api/storage/${target}/read/${bucketName}/${fileName}`);
          break;
        case 'qiniu':
          request({
            url: urlVideoServer + `/api/storage/qiniu/space_public/${bucketName}/${fileName}`,
            method: 'get',
          }).then(resolve).catch(reject);
          break;
      }
    });
  }

  /**
   * 获取可用的存储空间列表
   * */
  async listUploadTarget()
  {
    return request({
      url: urlVideoServer + '/api/target/list_all_targets',
      method: 'get',
    });
  }
}
