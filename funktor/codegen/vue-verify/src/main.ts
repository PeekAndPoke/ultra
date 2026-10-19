/**
 * The smallest app that consumes the generated insights pages, so `vite build` bundles them for real.
 *
 * `vue-tsc` cannot prove an SDK is consumable -- an unresolved import or a missing package only fails
 * the bundler. This entry reaches every tab, `NetworkGraph.vue` and its `vis-network` chunk.
 */
import { createApp } from 'vue'
import './funktorsdk/styles.ts'
import InsightsPage from './funktorsdk/insights/InsightsPage.vue'
import { sdkConfig } from './funktorsdk/runtime/client.ts'
import { provideSdkConfig } from './funktorsdk/ui/sdkContext.ts'

const app = createApp(InsightsPage)

provideSdkConfig(app, sdkConfig(window.location.origin))

app.mount('#app')
