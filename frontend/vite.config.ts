import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {

  const backendUrl = 'http://192.168.122.177:8080'
  const base = mode === 'production' ? '/new_front/' : '/'

  return {
    base,
    plugins: [
      vue(),
      vueJsx(),
      vueDevTools(),
      tailwindcss(),
    ],
    server: {
        proxy: {
          '/internal': {
            target: backendUrl,
            changeOrigin: true,
            secure: false,
          },
        },
      },
      resolve: {
        alias: {
          '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
      },
  }
})
