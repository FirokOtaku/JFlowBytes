// Vue.use(VueVideoPlayer.default);

/**
 * 清空一个数组
 * */
function clear(list)
{
    if (list.length)
        list.splice(0, list.length);
}

const app = new Vue({
    el: '#app',
    component: { VueVideoPlayer },
    data: {
        file: null,
        sliceSize: 5 * 1024 * 1024,

        inputPreview: {
            nameBucket: 'jfb',
            nameFile: 'be6eee25-c567-477e-91e6-6356ff45dea5.m3u8',
        },
        currentPreview: '',
        player: null,

        listUpload: [],

        listWorkflow: [],
        shouldRefreshListWorkflow: false,
        threadRefreshListWorkflow: 0,
        isRefreshingListWorkflow: false,
    },
    computed: {
        filename()
        {
            const { file } = this;
            return file?.name ?? 'unknown';
        },
        filesize()
        {
            const { file } = this;
            return file?.size ?? 0;
        },
        sliceCount()
        {
            const { filesize, sliceSize } = this;
            return Math.ceil(filesize / sliceSize);
        },
    },
    methods: {
        changeFile()
        {
            this.file = this.$refs['app-file'].files[0];
        },

        async changePreview(target)
        {
            let url = '';

            const { inputPreview } = this;
            const { nameBucket, nameFile } = inputPreview;
            switch(target)
            {
                case 'minio': case 'filesystem':
                    url = `http://localhost:29011/api/storage/${target}/read/${nameBucket}/${nameFile}`;
                    break;
                case 'qiniu':
                    let res = await axios({
                        url: `/api/storage/qiniu/space_public/${nameBucket}/${nameFile}`,
                        method: 'get',
                    });
                    url = res.data.data;
                    break;
            }

            this.currentPreview = url;

            const options = {
                controls: true,
                autoplay: false,
                autoSetup: true,
                responsive: true,
                fluid: true,
                sources: [ { src: url, } ],
            };
            if(this.player)
            {
                this.player.dispose();
            }
            const domPlayerParent = document.getElementById('dom-player-parent');
            if(document.getElementById('dom-player') == null)
            {
                domPlayerParent.innerHTML = `<video id="dom-player" class="video-js vjs-default-skin" controls responsive></video>`;
            }
            this.player = videojs('dom-player', options);
        },

        async uploadFile(target)
        {
            const { inputPreview } = this;
            const { nameBucket, nameFile } = inputPreview;
            const { file, filename, filesize, sliceSize, sliceCount } = this;
            const flow = {
                id: new Date().getTime(),
                idWorkflow: '',
                target,
                filename,
                filesize,
                sliceSize,
                sliceCount,
                shouldContinue: true,
                // -1: 上传完成
                // -2: 取消上传
                // -3: 上传失败
                // -4: 准备上传
                // 非负整数: 正在上传
                uploadCount: -4,
            };
            this.listUpload.push(flow);

            let idWorkflow = null;
            try
            {
                idWorkflow = await API.createWorkflow({
                    listOperation: [
                        "jfb:upload",
                        "jfb:file-merge",
                        "jfb:ffmpeg-transcode-m3u8",
                        `jfb:${target}-storage`,
                    ],
                    mapContextInitParam: {
                        'name_bucket': nameBucket.length ? nameBucket : 'jfb',
                        'count_slice': sliceCount,
                    },
                });
                flow.idWorkflow = idWorkflow;
            }
            catch (err)
            {
                alert('上传文件发生错误:\n' + err);
            }

            if(idWorkflow == null)
            {
                flow.uploadCount = -3;
                alert('无法获取任务id');
                return;
            }

            let indexSlice = 0;
            flow.uploadCount = 0;
            for(let slice of slicesOf(file, sliceSize))
            {
                if(!flow.shouldContinue)
                {
                    flow.uploadCount = -2;
                    break;
                }

                try
                {
                    let resUpload = await API.serviceUploadSlice({
                        idWorkflow,
                        indexSlice,
                        slice,
                    });
                    console.log('上传分片', resUpload.data);
                    indexSlice++;
                    flow.uploadCount++;
                }
                catch (e)
                {
                    flow.uploadCount = -3;
                    break;
                }
            }
            if(indexSlice === sliceCount)
                flow.uploadCount = -1;
        },

        stopUpload(flow)
        {
            flow.shouldContinue = false;
            API.deleteWorkflow(flow.idWorkflow)
                .then(()=>{})
                .catch((err)=>{
                    alert('中止上传失败, 这种情况可能需要再次请求中止工作流\n'+err);
                });

        },

        async refreshListWorkflow()
        {
            this.isRefreshingListWorkflow = true;
            try
            {
                let map = await API.listCurrentWorkflows();
                let list = [];
                for(let key of Object.keys(map))
                {
                    let value = map[key];
                    list.push({
                        id: key,
                        status: value,
                    });
                }
                clear(this.listWorkflow);
                this.listWorkflow.push(...list);
                this.isRefreshingListWorkflow = false;
            }
            catch (err)
            {
                console.log('加载工作流列表出错', err);
                this.isRefreshingListWorkflow = false;
            }
        },

        deleteWorkflow(id)
        {
            API.deleteWorkflow(id).finally(()=>{
                this.refreshListWorkflow().finally(()=>{});``
            });
        },

    },

    mounted() {
        let flag = 0;
        this.threadRefreshListWorkflow = setInterval(() => {
            flag++;
            if(flag % 5 !== 0) return;

            const { shouldRefreshListWorkflow } = this;
            if(shouldRefreshListWorkflow)
            {
                this.refreshListWorkflow().finally(()=>{});
            }
        }, 1000);
    },

});
















