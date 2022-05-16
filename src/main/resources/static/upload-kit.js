
function* taskHelloWorld()
{
    yield 'hw 1';
    yield 'hw 2';
    yield 'hw 3';
    yield 'hw 4';
}

function* taskWhile()
{
    let step = 0;
    while (true)
    {
        step++;
        yield step;

        if(step > 5)
            break;
    }
}

const { log } = console;

// let gen = taskWhile();
// setInterval(()=>{
//
//     let value = gen.next();
//     log('value', value);
// }, 1000);


/**
 * 创建上传任务
 * */
async function createTask(
    file, fileName, sliceSize
)
{
    const res = await axios({
        url: '/api/upload/createTask',
        method: 'post',
        data: {
            fileName,
            fileSize: file.size,
            sliceSize,
            type: 'Video_Slice',
        },
    });
    const id = res.data.data.id;

    if(id == null) throw '无法创建任务';

    // fixme 猜测是 http 最大连接数限制了这个代码的执行
    //       换用web socket可能好一些
    let threadTask = setInterval(()=>{
        axios({
            url: '/api/upload/queryTask',
            method: 'get',
            params: {
                idTask: id,
            },
        })
        .then(res => {
            task.process = res.data.data.process;
            // if(task.process === 'MergeSuccess')
            //     clearInterval(threadTask);
        })
        .catch(err => {
            log('发生错误');
        })
    }, 2000);


    const task = {
        id,
        process: '准备上传',
        threadStatus: threadTask,
    };


    const fileSize = file.size;
    const sliceCount = parseInt(fileSize / sliceSize + '') + (fileSize % sliceSize === 0 ? 0 : 1);
    for(let sliceIndex = 0; sliceIndex < sliceCount; sliceIndex++)
    {
        // 计算切片的起始位置
        const posStart = sliceIndex * sliceSize;
        const posStop = Math.min(fileSize, posStart + sliceSize);
        const filePart = file.slice(posStart, posStop);

        const form = new FormData();
        form.append('file', filePart);

        axios({
            url: `/api/upload/uploadSlice?taskId=${id}&sliceIndex=${sliceIndex}`,
            method: 'post',
            data: form,
        })
        .then(res => {
            let { process } = res.data;
            log(`上传切片 ${sliceIndex} 返回结果 ${process}`);
            task.process = process;
        })
        .catch(err => {
            log(`上传切片 ${sliceIndex} 发生错误 ${err}`);
        });
    }

    return task;
}
