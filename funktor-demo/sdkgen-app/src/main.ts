// Stylesheets FIRST, before anything renders. Every sheet any contributing module shipped, in
// cascade order — the file is generated, and it is emitted even when empty, so this line is stable.
import './funktorsdk/styles.ts'

import { createApp } from 'vue'
import App from './App.vue'
import { provideSdkConfig } from './funktorsdk/ui/sdkContext.ts'
import { router } from './router.ts'
import { config } from './sdk.ts'

const app = createApp(App)

// REQUIRED for contributed pages. `mountAll` hands the router a bare component, so a page shipped by
// a Kotlin module gets no props and cannot be given a client any other way. Without this line the
// insights page mounts and then fails on first render.
provideSdkConfig(app, config)

app.use(router).mount('#app')
