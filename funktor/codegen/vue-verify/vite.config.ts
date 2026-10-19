import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'

export default defineConfig({
    plugins: [vue()],
    build: {
        outDir: 'dist',
        emptyOutDir: true,
        // vis-network is one ~650 KB chunk, loaded only when a graph is opened. Above it, warn.
        chunkSizeWarningLimit: 700,
    },
    test: {
        environment: 'happy-dom',
        include: ['tests/**/*.spec.ts'],
        // JUnit XML next to the module's Kotlin results, so the counts are read the way the repo reads them.
        reporters: ['default', 'junit'],
        outputFile: { junit: '../build/test-results/vueVerify/TEST-vue-verify.xml' },
    },
})
