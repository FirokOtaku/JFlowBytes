
/**
 * 创建一个按照指定切片大小返回文件切片的迭代器
 * @param file {Blob} 文件
 * @param sliceSize {number} 切片大小. 不传默认为 5 MB
 * @returns {Iterator<Blob>} 切片迭代器
 * @example
 * for(const slice of slicesOf(file)) { ... }
 * */
function* slicesOf(file, sliceSize = 1024 * 1024 * 5)
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
 * 处理 firok.spring.bean.Ret 格式返回值
 * */
function _handleRet(res)
{
    const { data, success, msg } = res.data;
    if(!success)
        throw msg;
    else
        return data;
}
/**
 * 发起一次网络请求
 * */
function _axiosRequest(param)
{
    return new Promise((resolve, reject) => {
        axios(param)
        .then(res => resolve(_handleRet(res)))
        .catch(reject);
    });
}

const API = {

    async createWorkflow({ listOperation, mapContextInitParam, })
    {
        return _axiosRequest({
            method: 'post',
            url: '/api/workflow/create_workflow',
            data: {
                listOperation,
                mapContextInitParam,
            },
        });
    },

    async deleteWorkflow(idWorkflow)
    {
        return _axiosRequest({
            url: '/api/workflow/delete_workflow',
            method: 'delete',
            params: { idWorkflow, },
        });
    },

    async listCurrentWorkflows()
    {
        return _axiosRequest({
            url: '/api/workflow/list_current_workflow',
            method: 'post',
        });
    },

    async serviceUploadSlice({ idWorkflow, indexSlice, slice, })
    {
        const form = new FormData();
        form.append('file', slice);
        return _axiosRequest({
            url: '/api/workflow/service_upload_slice',
            method: 'post',
            params: { idWorkflow, indexSlice },
            data: form,
        });
    },
};

