package io.peekandpoke.ultra.codegen.sdk

import io.peekandpoke.ultra.codegen.model.TsTypeClaims
import io.peekandpoke.ultra.codegen.model.TsUrlParamClaims
import io.peekandpoke.ultra.codegen.model.TypeModel
import io.peekandpoke.ultra.codegen.model.TypeWalker
import io.peekandpoke.ultra.codegen.ts.TsBarrelEmitter
import io.peekandpoke.ultra.codegen.ts.TsMountEmitter
import io.peekandpoke.ultra.codegen.ts.TsModelEmitter
import io.peekandpoke.ultra.codegen.ts.TsStylesEmitter
import io.peekandpoke.ultra.slumber.SlumberConfig
import kotlin.reflect.KType

/** Roots contributed in phase 2, to be walked into the type model. */
class TsSdkRoots internal constructor(
    private val contributor: String,
    /**
     * Every URL-parameter claim, from every contributor.
     *
     * Complete by this phase, because all of phase 1 runs first — so a contributor may consult claims
     * it did not make itself, which is the point: the REST contributor types its parameters from
     * claims the datetime and vault contributors registered.
     */
    val urlParamClaims: TsUrlParamClaims = TsUrlParamClaims(),
) {

    private val roots = mutableListOf<TypeWalker.Root>()

    /** Adds [type] as a root, labelled for the "reached via" trail in error messages. */
    fun root(type: KType, label: String) {
        roots.add(TypeWalker.Root(type = type, label = label))
    }

    internal fun collected(): List<TypeWalker.Root> = roots.toList()

    /** The contributor these roots came from. */
    internal fun owner(): String = contributor
}

/** What a contributor gets during the emit phase. */
class TsSdkEmitContext internal constructor(
    /** The frozen, validated type model. */
    val model: TypeModel,
    /** Where to plan files, already scoped to the calling contributor. */
    val out: TsSdkOutput.Scope,
    /** Where to declare entries the BUILDER renders into a shared aggregate, e.g. page routes. */
    val registry: TsSdkRegistry.Scope,
)

/**
 * Runs the SDK generation phases.
 *
 * Takes a plain `List<TsSdkContributor>` rather than reaching into a container, so the whole generator
 * is unit-testable without one. The DI wiring lives entirely on the funktor side.
 */
class TsSdkBuilder(
    private val contributors: List<TsSdkContributor>,
    /**
     * The live serialization config, used to detect types whose JSON shape a custom codec reshapes.
     *
     * Required, deliberately. This used to default to null, which silently disabled the codec-parity
     * check — the one thing standing between a custom codec and a schema that does not describe what
     * the server writes, and the module's whole reason to exist. A default argument is not a place to
     * put a safety net's off switch.
     */
    private val slumberConfig: SlumberConfig,
) {
    companion object {
        /**
         * A builder for tests, using [SlumberConfig.default].
         *
         * The codec-parity check still RUNS; only the config is stock. There is deliberately no way to
         * turn the check off — a test whose fixtures cannot be slumbered is describing output no server
         * can produce, which is worth failing on.
         */
        fun forTesting(contributors: List<TsSdkContributor>): TsSdkBuilder =
            TsSdkBuilder(contributors = contributors, slumberConfig = SlumberConfig.default)
    }

    /** The outcome of a run. */
    data class Result(
        val model: TypeModel,
        val output: TsSdkOutput,
        val advisories: List<TsModelValidator.Advisory>,
    )

    /**
     * Runs all four phases and returns the planned output.
     *
     * Throws with an actionable message if validation fails; nothing is written in that case, because
     * [TsSdkOutput] only plans files.
     */
    fun build(): Result {
        check(contributors.isNotEmpty()) {
            "No TsSdkContributor is registered, so there is nothing to generate. Register at least one " +
                    "(the REST contributor is what supplies API endpoints)."
        }

        val duplicateNames = contributors.groupBy { it.name }.filterValues { it.size > 1 }.keys

        check(duplicateNames.isEmpty()) {
            "Contributor names must be unique — duplicated: ${duplicateNames.joinToString()}. Names " +
                    "identify contributors in conflict messages, so duplicates make those unreadable."
        }

        // Phase 1 — claims. Commutative: each contributor writes into a shared registry that rejects
        // a second claim for the same type.
        val claims = TsTypeClaims()

        contributors.forEach { it.claimTypes(claims.scopeFor(it.name)) }

        // Phase 1b — URL-parameter claims. A separate registry, because the two memberships are
        // independent rather than two renderings of one set; see TsUrlParamClaims.
        val urlParamClaims = TsUrlParamClaims()

        contributors.forEach { it.claimUrlParams(urlParamClaims.scopeFor(it.name)) }

        // Phase 2 — roots. Both claim registries are complete by now, so a contributor may consult
        // claims another one made.
        val rootsByContributor = contributors.map { contributor ->
            contributor.name to TsSdkRoots(contributor.name, urlParamClaims)
                .also { contributor.contribute(it) }
                .collected()
        }

        // A label is how a contributor asks for its root's resolved reference at emit time
        // (`TypeModel.refForRoot`), so a duplicate would silently hand one contributor another's type.
        val labelOwners = rootsByContributor
            .flatMap { (owner, roots) -> roots.map { it.label to owner } }
            .groupBy({ it.first }, { it.second })
            .filterValues { it.size > 1 }

        check(labelOwners.isEmpty()) {
            "Root labels must be unique — duplicated: " +
                    labelOwners.entries.joinToString { (label, owners) -> "'$label' by ${owners.joinToString()}" } +
                    ". A label identifies a root's resolved type at emit time, so a duplicate would " +
                    "give one contributor another's type. Fix: qualify the label with the " +
                    "contributor's own name."
        }

        val roots = rootsByContributor.flatMap { (_, roots) -> roots }

        check(roots.isNotEmpty()) {
            "No contributor supplied any root type, so the generated SDK would be empty. A contributor " +
                    "must call roots.root(type, label) during its contribute phase."
        }

        // Phase 3 — walk, then validate. Validation happens BEFORE any emit so a failure cannot leave
        // partially generated output.
        val model = TypeWalker(claims).walk(roots)

        val report = TsModelValidator(slumberConfig).validate(model)

        report.failIfProblems()

        // Phase 4 — emit.
        val output = TsSdkOutput()

        // The models file is the builder's own output rather than a contributor's: every contributor
        // feeds it, so no single one owns it.
        output.scopeFor("ultra:codegen").file(path = "models.ts", content = TsModelEmitter(model).emit())

        // Phase 4b — the aggregation registry. Collected during emit, rendered after, because an
        // aggregate is by definition not any one contributor's to write.
        val registry = TsSdkRegistry()

        contributors.forEach { contributor ->
            contributor.emit(
                TsSdkEmitContext(
                    model = model,
                    // The contributor's own loader, so `out.resource` finds resources shipped in the
                    // contributor's jar rather than only those on ultra:codegen's classpath.
                    out = output.scopeFor(contributor.name, contributor::class.java.classLoader),
                    registry = registry.scopeFor(contributor.name),
                )
            )
        }

        val registeredRoutes = registry.allRoutes()
        val registeredStyles = registry.allStyles()

        // A registered component or stylesheet that nobody emitted would surface as a
        // module-resolution error inside generated output — naming a file, not the contributor that
        // asked for it. The registry cannot see the output plan and the output cannot see the
        // registry, so this is the one place both are visible.
        val emitted = output.entries().map { it.path }.toSet()

        val missingComponents = registeredRoutes.filter { it.component !in emitted }

        check(missingComponents.isEmpty()) {
            "Registered route component(s) were never emitted: " +
                    missingComponents.joinToString { "'${it.component}' for '${it.path}' by '${it.declaredBy}'" } +
                    ". A contributor must emit the component it registers, typically with " +
                    "out.resource(...) in the same emit call."
        }

        val missingStyles = registeredStyles.filter { it.path !in emitted }

        check(missingStyles.isEmpty()) {
            "Registered stylesheet(s) were never emitted: " +
                    missingStyles.joinToString { "'${it.path}' by '${it.declaredBy}'" } +
                    ". A contributor must emit the stylesheet it registers, typically with " +
                    "out.sharedResource(...) in the same emit call."
        }

        // BOTH aggregates are emitted unconditionally, empty included. They are the app's stable
        // surface — `import { mountAll } from './funktorsdk/mount.ts'` and
        // `import './funktorsdk/styles.ts'` are hand-written lines in a file the generator must never
        // touch, so a file that appears only when some contributor happened to register something
        // would break the app's wiring on a profile change rather than on a code change.
        output.scopeFor("ultra:codegen").file(
            path = TsMountEmitter.PATH,
            content = TsMountEmitter.emit(registeredRoutes, registry.navRoutes()),
        )

        output.scopeFor("ultra:codegen").file(
            path = TsStylesEmitter.PATH,
            content = TsStylesEmitter.emit(registeredStyles),
        )

        // Without this, `styles.ts` does not compile as soon as it has a single import — TypeScript
        // does not know what a `.css` module is.
        output.scopeFor("ultra:codegen").file(
            path = TsStylesEmitter.TYPES_PATH,
            content = TsStylesEmitter.emitTypes(),
        )

        // Phase 5 — the barrel. LAST, because it re-exports what every contributor wrote, so it can
        // only be built once they have all run. The builder's own output for the same reason
        // `models.ts` is: every contributor feeds it, so no single one owns it.
        output.scopeFor("ultra:codegen").file(
            path = TsBarrelEmitter.PATH,
            content = TsBarrelEmitter.emit(output.entries().map { it.path }),
        )

        return Result(model = model, output = output, advisories = report.advisories)
    }
}
