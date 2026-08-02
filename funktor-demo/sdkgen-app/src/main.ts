// Stylesheets FIRST, before anything renders. Every sheet any contributing module shipped, in
// cascade order — the file is generated, and it is emitted even when empty, so this line is stable.
import './funktorsdk/styles.ts'

import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router.ts'

createApp(App).use(router).mount('#app')
