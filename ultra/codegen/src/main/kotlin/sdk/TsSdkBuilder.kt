package io.peekandpoke.ultra.codegen.sdk

import io.peekandpoke.ultra.codegen.model.TsTypeClaims
import io.peekandpoke.ultra.codegen.model.TypeModel
import io.peekandpoke.ultra.codegen.model.TypeWalker
import io.peekandpoke.ultra.codegen.ts.TsModelEmitter
import io.peekandpoke.ultra.slumber.SlumberConfig
import kotlin.reflect.KType

/** Roots contributed in phase 2, to be walked into the type model. */
class TsSdkRoots internal constructor(private val contributor: String) {

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
     * Null skips that check — acceptable in unit tests, never in a real run.
     */
    private val slumberConfig: SlumberConfig? = null,
) {
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

        // Phase 2 — roots.
        val roots = contributors.flatMap { contributor ->
            TsSdkRoots(contributor.name).also { contributor.contribute(it) }.collected()
        }

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

        contributors.forEach { contributor ->
            contributor.emit(
                TsSdkEmitContext(model = model, out = output.scopeFor(contributor.name))
            )
        }

        return Result(model = model, output = output, advisories = report.advisories)
    }
}
