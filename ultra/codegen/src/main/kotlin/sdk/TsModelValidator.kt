package io.peekandpoke.ultra.codegen.sdk

import io.peekandpoke.ultra.codegen.model.TsTypeDecl
import io.peekandpoke.ultra.codegen.model.TypeId
import io.peekandpoke.ultra.codegen.model.TypeModel
import io.peekandpoke.ultra.slumber.SlumberConfig
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.objects.DataClassSlumberer
import io.peekandpoke.ultra.slumber.builtin.objects.EnumCodec
import io.peekandpoke.ultra.slumber.builtin.objects.ObjectInstanceCodec
import io.peekandpoke.ultra.slumber.builtin.objects.ValueClassSlumberer
import io.peekandpoke.ultra.slumber.builtin.polymorphism.PolymorphicChildSlumberer
import io.peekandpoke.ultra.slumber.builtin.polymorphism.PolymorphicParentSlumberer
import kotlin.reflect.full.withNullability

/**
 * Checks a [TypeModel] before anything is emitted.
 *
 * This is the phase that stops a custom Slumber codec from silently producing a wrong TypeScript
 * type. `MpInstant` is the motivating case: it slumbers to `{ts, timezone, human}`, which nothing in
 * its `KType` reveals, so a structural walk would confidently emit the wrong thing. The old Dart
 * generator handled such types by pattern-matching classifiers, which meant a NEWLY added codec
 * silently produced wrong output. Here it fails the build.
 */
class TsModelValidator(
    /** The live serialization config, probed to detect types a custom codec reshapes. */
    private val slumberConfig: SlumberConfig,
) {
    /** A blocking problem. */
    data class Problem(
        val subject: String,
        val detail: String,
        val path: List<String>,
        val fix: String,
    )

    /** A non-blocking note surfaced in the run summary. */
    data class Advisory(
        val subject: String,
        val detail: String,
    )

    /** The outcome of validation. */
    data class Report(
        val problems: List<Problem>,
        val advisories: List<Advisory>,
    ) {
        val ok: Boolean get() = problems.isEmpty()

        /** Throws with a formatted, actionable message when there is at least one problem. */
        fun failIfProblems() {
            if (ok) return

            error(format())
        }

        /** Renders the problems the way they should appear in build output. */
        fun format(): String = buildString {
            append("[ultra:codegen] ")
            append(problems.size)
            append(if (problems.size == 1) " type has no valid TypeScript mapping:" else " types have no valid TypeScript mapping:")
            append("\n")

            problems.forEach { problem ->
                append("\n  ").append(problem.subject).append("\n")
                append("      ").append(problem.detail).append("\n")

                if (problem.path.isNotEmpty()) {
                    append("      reached via: ").append(problem.path.joinToString(" -> ")).append("\n")
                }

                append("      fix: ").append(problem.fix).append("\n")
            }
        }
    }

    fun validate(model: TypeModel): Report {
        val problems = buildList {
            addAll(unresolvedProblems(model))
            addAll(undeterminedProblems(model))
            addAll(nameCollisionProblems(model))
            addAll(danglingReferenceProblems(model))
            addAll(missingSchemaProblems(model))
            addAll(codecParityProblems(model))
        }

        val advisories = buildList {
            addAll(longAdvisories(model))
            addAll(opaqueAdvisories(model))
        }

        return Report(problems = problems, advisories = advisories)
    }

    /**
     * Positions where no type could be determined, so `unknown` would be emitted silently.
     *
     * A blocking problem, not an advisory. `unknown` accepts anything, so a schema carrying one has
     * quietly stopped validating that field — which is precisely the Dart generator's `dynamic`
     * fallback, the defect this module was built to remove. `claims.opaque` remains the deliberate,
     * reported way to ask for `unknown`.
     */
    private fun undeterminedProblems(model: TypeModel): List<Problem> = model.undetermined.map {
        Problem(
            subject = it.path.lastOrNull() ?: "<root>",
            detail = it.reason,
            path = it.path,
            fix = "use a concrete type here, or claim it explicitly — claims.opaque<T>(reason = ...) " +
                    "if 'unknown' really is the intent",
        )
    }

    /** Types the walker could not classify at all. */
    private fun unresolvedProblems(model: TypeModel): List<Problem> = model.unresolved.map {
        Problem(
            subject = it.id.key,
            detail = it.reason,
            path = it.path,
            fix = "claim it in a TsSdkContributor: claims.map<${it.id.simpleName}>(tsName = ..., from = ...), " +
                    "or claims.opaque<${it.id.simpleName}>(reason = ...)",
        )
    }

    /** Two distinct Kotlin types that would generate the same TypeScript name. */
    private fun nameCollisionProblems(model: TypeModel): List<Problem> =
        model.decls.values
            .groupBy { it.name }
            .filterValues { it.size > 1 }
            .map { (name, colliding) ->
                Problem(
                    subject = name,
                    detail = "TypeScript name collision between: " +
                            colliding.joinToString(", ") { it.id.key },
                    path = emptyList(),
                    fix = "claim one of them with an explicit distinct tsName",
                )
            }

    /** A reference pointing at a type that is neither declared nor claimed. */
    private fun danglingReferenceProblems(model: TypeModel): List<Problem> {
        val known: Set<TypeId> = model.decls.keys

        return model.decls.values
            .flatMap { decl -> decl.referencedIds().map { decl to it } }
            .filter { (_, ref) ->
                ref !in known && !model.usedClaims.containsKey(ref.cls.qualifiedName)
            }
            .distinctBy { (_, ref) -> ref.key }
            .map { (from, ref) ->
                Problem(
                    subject = ref.key,
                    detail = "referenced by '${from.name}' but never declared or claimed — internal " +
                            "walker invariant violation, please report it",
                    path = emptyList(),
                    fix = "none — this is a generator bug, not a usage error",
                )
            }
    }

    /** A non-opaque claim with no zod schema, which would leave `z.infer` with nothing to infer from. */
    private fun missingSchemaProblems(model: TypeModel): List<Problem> =
        model.usedClaims.values
            .filter { !it.opaque && it.schema == null }
            .map {
                Problem(
                    subject = it.qualifiedName,
                    detail = "claimed by '${it.claimedBy}' as TypeScript type '${it.tsName}' but with no " +
                            "zod schema, so the generated schema cannot reference it",
                    path = emptyList(),
                    fix = "pass schema = \"...\" alongside tsName in the claim",
                )
            }

    /**
     * The core check: does Slumber actually serialize each declared type the way the model assumes?
     *
     * Probes with the NULLABLE type deliberately — `SlumberModule.wrapIfNonNull` wraps a non-null
     * type's slumberer in `NonNullSlumberer`, whose inner slumberer is private and cannot be
     * unwrapped. Asking for the nullable variant returns the underlying slumberer directly.
     */
    private fun codecParityProblems(model: TypeModel): List<Problem> {
        val config = slumberConfig

        return model.decls.values.mapNotNull { decl ->
            val slumberer = runCatching {
                config.getSlumberer(decl.id.type.withNullability(true))
            }.getOrNull()

            when {
                slumberer == null -> Problem(
                    subject = decl.id.key,
                    detail = "Slumber has no slumberer for this type, so the server cannot serialize it " +
                            "at all — the generated type would describe output that never exists",
                    path = emptyList(),
                    fix = "give the type a SlumberModule, or stop exposing it through the API",
                )

                !matches(decl, slumberer) -> Problem(
                    subject = decl.id.key,
                    detail = "custom Slumber codec '${slumberer::class.simpleName}' handles this type, so " +
                            "its JSON shape cannot be derived from the type graph (the walker inferred " +
                            "'${decl::class.simpleName}')",
                    path = emptyList(),
                    fix = "claim it in a TsSdkContributor: " +
                            "claims.map<${decl.id.simpleName}>(tsName = ..., from = ..., schema = ...)",
                )

                else -> null
            }
        }
    }

    /** True when [slumberer] is the structural codec the declaration kind implies. */
    private fun matches(decl: TsTypeDecl, slumberer: Slumberer): Boolean = when (decl) {
        is TsTypeDecl.Obj ->
            slumberer is DataClassSlumberer ||
                    slumberer is PolymorphicChildSlumberer ||
                    slumberer is ObjectInstanceCodec

        is TsTypeDecl.Union -> slumberer is PolymorphicParentSlumberer

        is TsTypeDecl.EnumDecl -> slumberer is EnumCodec

        is TsTypeDecl.Alias -> slumberer is ValueClassSlumberer
    }

    private fun longAdvisories(model: TypeModel): List<Advisory> = model.longValued.map {
        Advisory(
            subject = it.path.joinToString(" -> "),
            detail = "Kotlin Long is emitted as TypeScript 'number'; JSON.parse loses precision above 2^53",
        )
    }

    private fun opaqueAdvisories(model: TypeModel): List<Advisory> =
        model.usedClaims.values.filter { it.opaque }.map {
            Advisory(
                subject = it.qualifiedName,
                detail = "emitted as 'unknown' — claimed opaque by '${it.claimedBy}': ${it.reason}",
            )
        }
}
