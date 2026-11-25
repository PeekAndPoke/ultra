import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

export default defineConfig({
    plugins: [vue()],
    server: {
        // Next in the demo's frontend port series: adminapp 36588, www 36589, ops 36590, b2b 36591, b2b2c 36592.
        port: 36593,
        // Fail instead of drifting to another port: the server's CORS list allows only this one.
        strictPort: true,
    },
})
