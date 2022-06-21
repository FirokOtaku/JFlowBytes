<style scoped>
.draggable
{
  position: fixed;
  cursor: move;
  user-select: none;
}
</style>

<template>
<div class="draggable"
     :style="{ top, left, right, bottom, }"
     @mousedown="mouseDown($event)"
     @mouseup="mouseUp($event)"
     @mouseleave="mouseUp($event)"
     @mousemove="mouseMove($event)"
>
  <slot></slot>
</div>
</template>

<script>
export default {
  name: "Draggable",
  props: {
    initTop: { type: Number, default: NaN },
    initLeft: { type: Number, default: NaN },
    initBottom: { type: Number, default: NaN },
    initRight: { type: Number, default: NaN },
    // initWidth: { type: Number, default: NaN },
    // initHeight: { type: Number, default: NaN },
  },
  data() {
    const { initTop, initLeft, initRight, initBottom, } = this;
    if(initTop == null && initBottom == null)
      throw '必须提供初始化纵坐标';
    if(initRight == null && initLeft == null)
      throw '必须提供初始化横坐标';
    return {
      topPx: isNaN(initTop) ? null : initTop,
      leftPx: isNaN(initLeft) ? null : initLeft,
      rightPx: isNaN(initRight) ? null : initRight,
      bottomPx: isNaN(initBottom) ? null : initBottom,
      isDragging: false,
    };
  },
  computed: {
    top()
    {
      const { topPx } = this;
      return topPx != null ? topPx + 'px' : undefined;
    },
    left()
    {
      const { leftPx } = this;
      return leftPx != null ? leftPx + 'px' : undefined;
    },
    bottom()
    {
      const { bottomPx } = this;
      return bottomPx != null ? bottomPx + 'px' : undefined;
    },
    right()
    {
      const { rightPx } = this;
      return rightPx != null ? rightPx + 'px' : undefined;
    },
  },

  methods: {
    mouseDown($event)
    {
      if(this.isDragging) return;
      this.isDragging = true;
    },
    mouseUp()
    {
      this.isDragging = false;
    },
    mouseMove($event)
    {
      if(!this.isDragging) return;
      const xMov = $event.movementX;
      const yMov = $event.movementY;
      if(xMov !== 0)
      {
        const { leftPx, rightPx } = this;
        if(leftPx != null) this.leftPx += xMov;
        else this.rightPx -= xMov;
      }
      if(yMov !== 0)
      {
        const { topPx, bottomPx } = this;
        if(topPx != null) this.topPx += yMov;
        else this.bottomPx -= yMov;
      }
    },
  },
}
</script>






