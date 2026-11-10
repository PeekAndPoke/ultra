/**
 * An app with its OWN global Prism keeps it when the insights CodeBlock loads Prism (ULTRA-00190). Its own file:
 * the global must exist before the first load, and the load is once per module graph.
 */
import { afterEach, describe, expect, it, vi } from 'vitest'
import CodeBlock, { loadPrism } from '../src/funktorsdk/insights/CodeBlock.vue'
import { highlighted, mount } from './support.ts'
import type { Mounted } from './support.ts'

const appsPrism = vi.hoisted(() => {
    const own = { languages: { appsOwnLanguage: {} }, manual: false }
    ;(globalThis as { Prism?: unknown }).Prism = own
    return own
})

let mounted: Mounted | null = null

/** The app's own code block, which the app's Prism highlighted its own way. */
function appsCodeBlock(): HTMLElement {
    const code = document.createElement('code')
    code.className = 'language-kotlin'
    code.innerHTML = '<span class="token keyword">val</span> x = 1'
    document.body.appendChild(code)
    return code
}

afterEach(() => {
    mounted?.unmount()
    mounted = null
})

describe('an app with its own Prism', () => {
    it('gets its global back, languages and all, and the block is still highlighted', async () => {
        const code = appsCodeBlock()
        const before = code.innerHTML

        // While the chunk downloads, the stand-in inherits from the app's Prism: the app keeps working
        const pending = loadPrism()
        expect((globalThis as { Prism?: { manual?: boolean, languages?: unknown } }).Prism?.languages).toBe(appsPrism.languages)
        expect((globalThis as { Prism?: { manual?: boolean } }).Prism?.manual).toBe(true)
        await pending

        mounted = await mount(CodeBlock, { text: 'FOR d IN c RETURN d', language: 'aql' })
        await highlighted()
        await new Promise((resolve) => setTimeout(resolve, 50))

        // Our copy loaded manual, not with the app's `manual: false`: it highlighted none of the app's blocks
        expect(code.innerHTML).toBe(before)

        expect((globalThis as { Prism?: unknown }).Prism).toBe(appsPrism)
        expect(appsPrism.languages).toEqual({ appsOwnLanguage: {} })
        expect([...mounted.host.querySelectorAll('.token.keyword')].map((it) => it.textContent)).toEqual(['FOR', 'IN', 'RETURN'])
    })
})
