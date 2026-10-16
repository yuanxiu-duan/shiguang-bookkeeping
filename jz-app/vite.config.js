import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

/**
 * 注入 crypto polyfill 到每个 JS 模块顶部
 * 解决 node-forge / crypto-js 在小程序/App 环境中访问 window.crypto 报错的问题
 */
function cryptoPolyfill() {
  const PRELUDE = [
    'if(typeof globalThis!=="undefined"){',
      'if(!globalThis.window)globalThis.window=globalThis;',
      'if(!globalThis.self)globalThis.self=globalThis;',
      'var _cpf=globalThis;',
      'if(!_cpf.crypto)_cpf.crypto={getRandomValues:function(arr){',
        'for(var i=0;i<arr.length;i++)arr[i]=Math.floor(Math.random()*256);',
        'return arr;',
      '}};',
    '}',
  ].join('')

  return {
    name: 'crypto-polyfill-inject',
    enforce: 'pre',
    transform(code, id) {
      // 只注入业务代码 + node-forge + crypto-js，跳过 CSS/HTML/其他
      if (/\.(js|ts|mjs|cjs)$/.test(id) && !id.includes('node_modules/@dcloudio')) {
        return PRELUDE + '\n' + code
      }
    }
  }
}

export default defineConfig({
  plugins: [cryptoPolyfill(), uni()],
  server: {
    port: 6174,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://localhost:8070',
        changeOrigin: true,
        rewrite: (path) => path
      }
    }
  }
})