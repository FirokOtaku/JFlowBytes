
Vue.component('vjs', {
    template: `<div style="width: 400px; height: 300px">
    <video ref="vjs"
    class="video-js" controls></video>
</div>`,
    props: {
        options: {
            type: Object,
            default: () => {
                return {}
            },
        },
    },
    data() {
        return {
            player: null,
        };
    },
    mounted() {
        this.player = videojs(
            this.$refs['vjs'],
            this.options,
            ()=>{
                console.log('vjs component mounted and ready');
            }
        );
    },
    beforeDestroy() {
        if (this.player) {
            this.player.dispose();
        }
    },
});

const app = new Vue({
    el: '#app',
    data: {
        sliceSize: 5 * 1024 * 1024,
        fileObject: null,
        hasStartUpload: false,
        taskUpload: null,

        opt: {
            autoplay: true,
            controls: true,
            sources: [
                {
                    src: 'http://localhost:29011/api/fs/minio/2699a00c-75b1-4f02-91f4-d0dc9c8d9de1.m3u8',
                    type: 'application/x-mpegURL'
                }
            ]
        }
    },
    computed: {
        fileName() {
            const { fileObject } = this;
            return fileObject?.name ?? '未选中';
        },
        fileSize() {
            const { fileObject } = this;
            return fileObject?.size ?? 0;
        },
        sliceCount() {
            const { fileSize, sliceSize } = this;
            return fileSize !== 0 && sliceSize !== 0 ?
                parseInt(fileSize / sliceSize + '') + (fileSize % sliceSize === 0 ? 0 : 1) :
                0;
        },
    },
    methods: {
        async startUploadTask() {
            const { hasStartUpload, fileObject, sliceSize, fileSize, fileName, } = this;
            if(hasStartUpload) return;
            this.hasStartUpload = true;

            this.taskUpload = await createTask(
                fileObject,
                fileName,
                sliceSize,
            );
        },
        stopUploadTask() {
            const thread = this.taskUpload?.threadStatus;
            clearInterval(thread);
        },
    },
});

