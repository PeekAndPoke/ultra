<script lang="ts">
/**
 * Prism, loaded once per page on first use, without side effects on the host page.
 *
 * Module scope (a plain `<script>`), so every CodeBlock shares one load. Two things Prism does on load that an
 * SDK component must not do to an app:
 *
 * - It highlights every `language-*` element of the page, unless `Prism.manual` was set BEFORE it loaded.
 * - It sets the global `Prism`, which an app with its own Prism would lose, languages and all.
 *
 * Prism copies `manual` from whatever global it finds, so a stand-in with `manual: true` is the global for the
 * duration of the load, ALWAYS: an app's own Prism in auto mode would otherwise hand its `manual: false` to our
 * copy, which then re-highlights the app's code blocks with none of its languages. The stand-in inherits from the
 * app's Prism, so the app can still highlight and register languages while the chunk downloads. Afterwards the
 * app's global is put back, or there is none again; this component uses the module, never the global. Accepted:
 * an app that sets a NEW global Prism during the download loses it, because Prism overwrites the global when it
 * evaluates. Lazy like the graph library: the tab renders plain text until Prism is there, and stays plain text if
 * it never comes.
 *
 * The two grammars are inlined rather than imported from `prismjs/components/`: those files register through
 * the global and ship no types. Copied from prismjs 1.30.0 (`prism-json.js`, `prism-aql.js`), MIT, Copyright
 * (c) 2012 Lea Verou.
 */
import type { Grammar } from 'prismjs'

interface PrismApi {
    tokenize(text: string, grammar: Grammar): Array<string | PrismToken>
}

export interface PrismToken {
    type: string
    content: string | PrismToken | Array<string | PrismToken>
    alias?: string | string[]
}

const JSON_GRAMMAR: Grammar = {
    property: { pattern: /(^|[^\\])"(?:\\.|[^\\"\r\n])*"(?=\s*:)/, lookbehind: true, greedy: true },
    string: { pattern: /(^|[^\\])"(?:\\.|[^\\"\r\n])*"(?!\s*:)/, lookbehind: true, greedy: true },
    comment: { pattern: /\/\/.*|\/\*[\s\S]*?(?:\*\/|$)/, greedy: true },
    number: /-?\b\d+(?:\.\d+)?(?:e[+-]?\d+)?\b/i,
    punctuation: /[{}[\],]/,
    operator: /:/,
    boolean: /\b(?:false|true)\b/,
    null: { pattern: /\bnull\b/, alias: 'keyword' },
}

const AQL_GRAMMAR: Grammar = {
    comment: /\/\/.*|\/\*[\s\S]*?\*\//,
    property: {
        pattern: /([{,]\s*)(?:(?!\d)\w+|(["'´`])(?:(?!\2)[^\\\r\n]|\\.)*\2)(?=\s*:)/,
        lookbehind: true,
        greedy: true,
    },
    string: { pattern: /(["'])(?:(?!\1)[^\\\r\n]|\\.)*\1/, greedy: true },
    identifier: { pattern: /([´`])(?:(?!\1)[^\\\r\n]|\\.)*\1/, greedy: true },
    variable: /@@?\w+/,
    keyword: [
        { pattern: /(\bWITH\s+)COUNT(?=\s+INTO\b)/i, lookbehind: true },
        /\b(?:AGGREGATE|ALL|AND|ANY|ASC|COLLECT|DESC|DISTINCT|FILTER|FOR|GRAPH|IN|INBOUND|INSERT|INTO|K_PATHS|K_SHORTEST_PATHS|LET|LIKE|LIMIT|NONE|NOT|NULL|OR|OUTBOUND|REMOVE|REPLACE|RETURN|SHORTEST_PATH|SORT|UPDATE|UPSERT|WINDOW|WITH)\b/i,
        { pattern: /(^|[^\w.[])(?:KEEP|PRUNE|SEARCH|TO)\b/i, lookbehind: true },
        { pattern: /(^|[^\w.[])(?:CURRENT|NEW|OLD)\b/, lookbehind: true },
        { pattern: /\bOPTIONS(?=\s*\{)/i },
    ],
    function: /\b(?!\d)\w+(?=\s*\()/,
    boolean: /\b(?:false|true)\b/i,
    range: { pattern: /\.\./, alias: 'operator' },
    number: [/\b0b[01]+/i, /\b0x[0-9a-f]+/i, /(?:\B\.\d+|\b(?:0|[1-9]\d*)(?:\.\d+)?)(?:e[+-]?\d+)?/i],
    operator: /\*{2,}|[=!]~|[!=<>]=?|&&|\|\||[-+*/%]/,
    punctuation: /::|[?.:,;()[\]{}]/,
}

/**
 * Longer text is not highlighted. Prism's greedy patterns rescan from each token, so text built to be many
 * unterminated comments (`/*` in every value of an `$in`, repeated in an explain plan) costs quadratic time on
 * the main thread. Queries and capped values are far below this; a huge plan reads fine as plain text.
 */
export const MAX_HIGHLIGHT_CHARS = 32_000

/** The languages this block highlights; anything else renders as plain text. */
export const GRAMMARS: Readonly<Record<string, Grammar>> = { json: JSON_GRAMMAR, aql: AQL_GRAMMAR }

let loading: Promise<PrismApi> | null = null

export function loadPrism(): Promise<PrismApi> {
    loading ??= (async () => {
        const host = globalThis as { Prism?: unknown }
        const before = host.Prism
        const parent = typeof before === 'object' && before !== null ? before : null
        const standIn: unknown = Object.assign(Object.create(parent), { manual: true, disableWorkerMessageHandler: true })

        host.Prism = standIn

        try {
            return ((await import('prismjs')) as { default: PrismApi }).default
        } finally {
            if (before === undefined) delete host.Prism
            else host.Prism = before
        }
    })()

    return loading
}
</script>

<script setup lang="ts">
/**
 * Highlighted code: a query, its values, its plan (ULTRA-00190). The old GUI ran these through Prism.
 *
 * **No `v-html`.** Prism's `highlight()` returns HTML; this renders `tokenize()`'s token tree as Vue elements
 * instead, so every piece of text is a text node and Vue escapes it. The text is attacker-controlled: a query
 * holds what a caller sent. Unknown languages, and the time before Prism loads, render the text as it is.
 *
 * No `<style>` block: the token colours are in `insights.css`, from the theme's variables.
 */
import { defineComponent, h, ref, watch } from 'vue'
import type { PropType, Ref, VNodeChild } from 'vue'

defineOptions({ name: 'CodeBlock' })

const props = withDefaults(
    defineProps<{
        text?: string | null
        /** `json` or `aql`; anything else renders unhighlighted. */
        language?: string | null
        emptyText?: string
        wrap?: boolean
    }>(),
    { text: null, language: null, emptyText: 'Nothing recorded', wrap: false },
)

type Stream = Array<string | PrismToken>

const tokens: Ref<Stream> = ref([])

watch(
    () => [props.text, props.language],
    async () => {
        const text = props.text ?? ''
        const language = props.language
        tokens.value = [text]

        const grammar = language !== null && Object.hasOwn(GRAMMARS, language) ? GRAMMARS[language] : undefined
        if (text === '' || grammar === undefined || text.length > MAX_HIGHLIGHT_CHARS) return

        try {
            const prism = await loadPrism()

            // Still the same input: a later change may have overtaken this load
            if (text === (props.text ?? '') && language === props.language) tokens.value = prism.tokenize(text, grammar)
        } catch {
            // Plain text it stays
        }
    },
    { immediate: true },
)

function classesOf(token: PrismToken): string[] {
    const aliases = token.alias === undefined ? [] : Array.isArray(token.alias) ? token.alias : [token.alias]
    return ['token', token.type, ...aliases]
}

function render(content: string | PrismToken | Stream): VNodeChild {
    if (typeof content === 'string') return content
    if (Array.isArray(content)) return content.map(render)
    return h('span', { class: classesOf(content) }, [render(content.content)])
}

/** The token tree as elements; strings are text nodes, which Vue escapes. */
const Tokens = defineComponent({
    props: { stream: { type: Array as PropType<Stream>, required: true } },
    setup: (inner) => () => render(inner.stream),
})
</script>

<template>
    <pre v-if="text" class="fk-pre fk-code" :class="{ 'fk-pre--wrap': wrap }"><code><Tokens :stream="tokens" /></code></pre>
    <div v-else class="fk-empty">{{ emptyText }}</div>
</template>
