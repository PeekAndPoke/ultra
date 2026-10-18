import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

export default defineConfig({
    plugins: [vue()],
    server: {
        // Next in the demo's frontend port series: adminapp 36588, www 36589, ops 36590.
        port: 36591,
    },
})
