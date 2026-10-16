import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn.mjs'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router/index.js'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 全局未捕获 Promise 错误调试
window.addEventListener('unhandledrejection', event => {
  console.error('[UnhandledRejection]', event.reason)
  console.error('[Stack]', event.reason?.stack || event.reason)
  // 阻止默认控制台输出
  event.preventDefault()
})

app.mount('#app')
