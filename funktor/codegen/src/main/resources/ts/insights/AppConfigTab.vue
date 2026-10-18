<script setup lang="ts">
/**
 * The `app-config` slice: `AppInfo` and the whole application config.
 *
 * **The collector that cannot be typed.** Both fields are `Any` in Kotlin and arrive as free-form JSON,
 * so a JSON tree is the honest rendering rather than a fallback.
 *
 * ## Why there is a warning in the UI and not just in a comment
 *
 * This tab was blocked until 2026-08-01, when `Redacted<T>` closed four confirmed leaks -- the JWT
 * signing key, the CSRF secret, the Arango password and the Mongo connection string were being written
 * verbatim into every record. `Redacted` redacts at the SERIALIZER, so those four are now
 * `***redacted***` here.
 *
 * What it cannot do is protect a field it does not annotate, and this collector serialises the entire
 * `AppConfig` as `Any`. **An application's own secret held in a plain `String` still reaches every
 * record and would be rendered here.** That is a property of the app's config, not of the framework, so
 * the framework cannot fix it -- it can only say so where someone reading the screen will see it.
 *
 * See `RequestTab.vue` for the import-path note.
 */
import { computed } from 'vue'
import JsonTree from '../ui/JsonTree.vue'
import { readAppConfig } from './slices.ts'

defineOptions({ name: 'AppConfigTab' })

const props = defineProps<{ data: unknown }>()

const slice = computed(() => readAppConfig(props.data))
</script>

<template>
    <div v-if="slice !== null">
        <div class="fk-notice">
            Values declared as <code>Redacted</code> appear as <code>***redacted***</code>. Anything else
            is shown verbatim -- a secret held in a plain <code>String</code> is recorded and displayed.
        </div>

        <section class="fk-section">
            <h4 class="fk-section__title">App info</h4>
            <JsonTree :value="slice.info" :expand-depth="2" />
        </section>

        <section class="fk-section">
            <h4 class="fk-section__title">Config</h4>
            <JsonTree :value="slice.config" :expand-depth="1" />
        </section>
    </div>

    <JsonTree v-else :value="data" :expand-depth="2" />
</template>
