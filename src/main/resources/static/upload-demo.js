
const app = new Vue({
    el: '#app',
    data: {
        sliceSize: 5 * 1024 * 1024,
        fileObject: null,
        hasStartUpload: false,
        taskUpload: null,
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

