package io.peekandpoke.ultra.common.slumber

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.reflect.KClass

/**
 * Namespace for slumber-related annotations.
 *
 * Lives in `ultra:common`, not `ultra:slumber`, so that a type can both CARRY an annotation here and be
 * handled by a codec in `ultra:slumber` — the other way round is a dependency cycle, since `ultra:slumber`
 * already depends on `ultra:common`. These are contracts read by slumber, two KSP processors and the
 * TypeScript generator; no machinery lives here.
 *
 * `@Target()` is empty, so [Slumber] itself cannot be applied to any declaration.
 */
@Target()
annotation class Slumber {

    /**
     * Marks a property or field for inclusion in serialization output, even if it is not a constructor parameter.
     *
     * `ANNOTATION_CLASS` is a valid target too: other annotations can carry `@Slumber.Field` themselves to opt
     * their annotated properties in via meta-annotation (see `DataClassSlumberer.hasAnyAnnotationRecursive`).
     */
    @Target(FIELD, PROPERTY, ANNOTATION_CLASS)
    annotation class Field

    /**
     * Declares the wire shape a custom-coded type slumbers to, so consumers can read it instead of
     * mirroring it by hand.
     *
     * ```kotlin
     * @Slumber.As(MpDateTimeRawData::class)  data class MpInstant(...)   // an object
     * @Slumber.As(Long::class)               data class MpLocalTime(...) // a bare scalar
     * ```
     *
     * ### It is DESCRIPTIVE, not generative
     *
     * The codec is not derived from this and does not read it — the type keeps whatever `Slumberer` its
     * `SlumberModule` provides. Keeping the declaration true to the codec is the type author's job, the
     * same way the codec itself is. `SlumberAsRoundTripSpec` checks every annotated type in this repo.
     *
     * ### It describes the SLUMBER direction only
     *
     * The two directions can differ. `Redacted<T>` slumbers to a `String` placeholder but awakes from
     * `T`'s own shape, so `@Slumber.As(String::class)` states the truth about output and says nothing
     * about input. Consumers that generate query paths or read models want exactly the output shape;
     * one generating a write model must not assume this is it.
     *
     * ### It is never the only source, and consumers disagree about the fallback
     *
     * Types nobody here owns — `java.time.*`, `kotlinx.datetime.*` — are custom-coded too and cannot be
     * annotated. What happens then is NOT uniform, so do not assume it:
     *
     * - `ultra:codegen` falls back to a registry, `TsTypeClaims`.
     * - The karango and monko KSP processors have no registry. They SUPPRESS instead: those packages
     *   are blacklisted, so an unannotated third-party type generates no query paths at all. That is
     *   silent-but-safe — no accessor rather than a wrong one.
     *
     * ### A replaced codec silently invalidates this
     *
     * The declaration sits on the type; the shape is produced by whichever codec `SlumberConfig` selects,
     * and `prependModules` lets an application put its own module ahead of the built-in one. Replace the
     * codec for an annotated type and every generated query path for it becomes wrong, with nothing to
     * detect it — the annotation cannot know it was overridden. Nothing in this repo does that today
     * (the DB drivers prepend only their own `Ref`/`Stored` module), but an application can.
     */
    // RUNTIME is Kotlin's default and is stated anyway because it is load-bearing: `ultra:codegen` reads
    // this reflectively at run time, while the KSP processors read it at build time.
    @Target(CLASS)
    @Retention(RUNTIME)
    annotation class As(val shape: KClass<*>)
}
