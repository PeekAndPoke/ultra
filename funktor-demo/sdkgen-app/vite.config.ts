import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

export default defineConfig({
    plugins: [vue()],
    server: {
        port: 5273,
        // The demo server. Proxying keeps the SDK's baseUrl a plain '' so the browser makes
        // same-origin requests and CORS never enters the picture for a local demo.
        proxy: {
            '/api': { target: 'http://localhost:8337', changeOrigin: true },
            '/_': { target: 'http://localhost:8337', changeOrigin: true },
        },
    },
})
