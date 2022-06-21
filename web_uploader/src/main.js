

// import axios from "axios";


import videojs from 'video.js';
import 'video.js/dist/video-js.css';
window.videojs = videojs;
require('video.js/dist/lang/zh-CN.js');

// main.ts
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import { createApp } from 'vue'
import App from './App.vue'

const app = createApp(App);
app.use(ElementPlus, { locale: zhCn });
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
    // console.log('key', key , component)
}
// app.config.errorHandler = () => {};
// app.config.warnHandler = () => {};

app.mount('#app');
