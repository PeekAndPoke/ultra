/**
 * Executes the TypeScript emitted by ultra:codegen against real Slumber output.
 *
 * `tsc --noEmit` (run first by the `verify` script) proves the generated code type-checks.
 * This proves it also BEHAVES: that each schema accepts the JSON Slumber actually writes, and —
 * just as important — that it rejects malformed input. A schema that accepts everything would
 * type-check perfectly and be worthless.
 *
 * Everything is driven by `generated/manifest.json`, which the Kotlin side emits alongside the
 * fixtures. Nothing here is hand-maintained per fixture, so the negative cases cannot drift away
 * from the model the way a hand-written list would.
 */
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { verifyRuntime } from './verifyRuntime.ts'

interface Fixture {
    /** Fixture name, matching `<name>.ts` and `<name>.sample.json`. */
    readonly name: string
    /** The exported schema to parse with. */
    readonly schema: string
    /** Field names the schema must require — used to build drop-one-key negative cases. */
    readonly requiredFields: readonly string[]
}

interface Manifest {
    readonly fixtures: readonly Fixture[]
}

interface Schema {
    parse(value: unknown): unknown
}

const GENERATED = join(import.meta.dirname, 'generated')

const manifest: Manifest = JSON.parse(readFileSync(join(GENERATED, 'manifest.json'), 'utf8'))

let failures = 0

function report(ok: boolean, label: string, detail: string = ''): void {
    if (ok) {
        console.log(`  ok    ${label}`)
    } else {
        failures++
        console.log(`  FAIL  ${label}${detail ? ` — ${detail}` : ''}`)
    }
}

/** Runs [fn], returning whether it threw. */
function rejected(fn: () => unknown): boolean {
    try {
        fn()
        return false
    } catch {
        return true
    }
}

for (const fixture of manifest.fixtures) {
    // Isolated per fixture: a module that fails to load must be reported, not abort the whole run and
    // take every later fixture's checks with it.
    let module: Record<string, unknown>

    try {
        module = await import(`./generated/${fixture.name}.ts`)
    } catch (e) {
        report(false, `${fixture.name}: module failed to load`, (e as Error).message.split('\n')[0])
        continue
    }

    const schema = module[fixture.schema] as Schema | undefined

    if (!schema || typeof schema.parse !== 'function') {
        report(false, `${fixture.name}: export '${fixture.schema}' is missing or not a schema`)
        continue
    }

    const sample: unknown = JSON.parse(
        readFileSync(join(GENERATED, `${fixture.name}.sample.json`), 'utf8'),
    )

    // 1. The schema must accept exactly what Slumber wrote.
    try {
        schema.parse(sample)
        report(true, `${fixture.name}: parses real Slumber output`)
    } catch (e) {
        report(false, `${fixture.name}: parses real Slumber output`, (e as Error).message.split('\n')[0])
    }

    // 2. It must reject non-objects. Catches a schema that degenerated to z.unknown() or z.any().
    report(
        rejected(() => schema.parse(42)) && rejected(() => schema.parse(null)),
        `${fixture.name}: rejects a non-object`,
    )

    // 3. Dropping any required key must be rejected. This is the check that proves the field set is
    //    real rather than decorative, and it needs no per-fixture handwriting.
    for (const field of fixture.requiredFields) {
        const { [field]: _dropped, ...withoutField } = sample as Record<string, unknown>

        report(
            rejected(() => schema.parse(withoutField)),
            `${fixture.name}: rejects missing '${field}'`,
        )
    }
}

await verifyRuntime(report, GENERATED)

if (manifest.fixtures.length === 0) {
    console.log('  FAIL  manifest contained no fixtures — the generator produced nothing to verify')
    failures++
}

console.log(
    failures === 0
        ? `\nAll TypeScript verification passed (${manifest.fixtures.length} fixtures).`
        : `\n${failures} TypeScript verification failure(s).`,
)

process.exit(failures === 0 ? 0 : 1)
