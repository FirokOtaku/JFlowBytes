<style scoped>

</style>

<template>
<span>
  <span v-if="target === 'minio'">MinIO</span>
  <span v-else-if="target === 'qiniu'">七牛云</span>
  <span v-else-if="target === 'filesystem'">本地文件存储</span>
  <span v-else>未知</span>
</span>
</template>

<script>
export default {
  name: "uploadTarget",
  props: {
    target: { type: String, default: '' }
  },
}
</script>
