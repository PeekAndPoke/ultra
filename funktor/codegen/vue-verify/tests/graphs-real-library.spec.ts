/**
 * The REAL `vis-network`, unmocked. happy-dom has no canvas, so drawing fails -- which makes this the
 * test of the promise that a broken graph never takes its tab with it.
 */
import { afterEach, describe, expect, it } from 'vitest'
import KontainerTab from '../src/funktorsdk/insights/KontainerTab.vue'
import VaultTab from '../src/funktorsdk/insights/VaultTab.vue'
import { click, mount, recorded, recordedGraphs, settle, sliceOf } from './support.ts'
import type { Mounted } from './support.ts'

let mounted: Mounted | null = null

afterEach(() => {
    mounted?.unmount()
    mounted = null
})

describe('when the graph library itself fails', () => {
    it('the kontainer tab shows why, and keeps its table', async () => {
        const slice = sliceOf(recorded.fat(), 'kontainer')
        mounted = await mount(KontainerTab, { data: slice })

        await click(mounted.host, 'Show Full Graph')
        await settle()

        expect(mounted.host.querySelector('.fk-graph .fk-notice--error')?.textContent).toContain('could not be drawn')
        expect(mounted.host.querySelectorAll('.fk-services tbody tr')).toHaveLength(slice.info.services.length)
    })

    it('the vault tab shows why, and keeps its query cards', async () => {
        const slice = sliceOf(recorded.vault(), 'vault')
        mounted = await mount(VaultTab, { data: slice }, recordedGraphs().graphs)

        await click(mounted.host, 'Show Database Graph')
        await settle()

        expect(mounted.host.querySelector('.fk-graph .fk-notice--error')?.textContent).toContain('could not be drawn')
        expect(mounted.host.querySelectorAll('.fk-query')).toHaveLength(slice.entries.length)
    })
})
