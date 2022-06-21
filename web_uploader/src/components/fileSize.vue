<style scoped>

</style>

<template>
<span>
  <span v-if="size < 1024">
    {{ size }} B
  </span>
  <span v-else-if="size < 1024 * 1024">
    {{ (size / 1024).toFixed(2) }} KB
  </span>
  <span v-else>
    {{ (size / 1024 / 1024).toFixed(2) }} MB
  </span>
</span>
</template>

<script>
export default {
  name: "fileSize",
  props: {
    size: {
      type: Number,
      default: 0,
    },
  },
}
</script>
