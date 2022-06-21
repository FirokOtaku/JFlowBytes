<style scoped>
#ball-base
{
  border: 1px #53d4ff solid;
  width: 85px;
  height: 110px;
  border-radius: 100px;
  background: linear-gradient(#fbfeff, #53d4ff);
}
</style>

<template>
  <draggable id="ball-base"
             :init-top="200"
             :init-right="200">
    <el-progress type="circle"
                 :width="70"
                 style="margin-top: 8px"
                 :percentage="percentage"
                 :stroke-width="2.5"
                 :color="colorBar"
                 :status="status"
    />
    <div style="font-size: 8px">
      <div v-if="speed > 0">
        <fileSize :size="speed"/>/s
      </div>
      <div v-else>
        -
      </div>
    </div>
  </draggable>
</template>

<script>
import Draggable from "./Draggable";
import fileSize from "./fileSize";

export default {
  name: "UploadStatusBall",
  components: { Draggable, fileSize, },
  props: {
    value: { type: Number, default: 0 },
    status: { type: String, default: undefined },
    colorBar: { type: String, default: '#1d92fe' },
    speed: { type: Number, default: 0, },
  },
  computed: {
    percentage() { return this.value < 0 ? 0 : this.value > 100 ? 100 : this.value; },
  },
}
</script>
