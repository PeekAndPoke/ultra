package io.peekandpoke.ultra.codegen.sdk

import io.peekandpoke.ultra.codegen.model.TsTypeClaims

/**
 * Extension point for everything that feeds the TypeScript SDK generator.
 *
 * The CONTRACT is phased, not the contributors. [TsSdkBuilder] runs all [claimTypes], then all
 * [contribute], then validates, then all [emit]. Within a phase every operation is a keyed insert, so
 * it commutes — which makes contributor order structurally irrelevant rather than merely
 * conventional. That matters because contributors arrive from a DI container, which guarantees no
 * ordering at all.
 *
 * Implementations are registered as singletons and injected as a list; see the funktor-side module.
 */
interface TsSdkContributor {

    /** Identifies this contributor in conflict messages and in the run summary. */
    val name: String

    /**
     * Phase 1 — declare how Kotlin types this contributor owns appear in TypeScript.
     *
     * For anything whose JSON shape cannot be derived from the type graph, i.e. anything behind a
     * custom Slumber codec.
     */
    fun claimTypes(claims: TsTypeClaims.Scope) {}

    /**
     * Phase 2 — contribute roots to be walked, such as API endpoints or extra types.
     */
    fun contribute(roots: TsSdkRoots) {}

    /**
     * Phase 4 — emit files. The model is frozen and has passed validation; nothing can be added.
     */
    fun emit(context: TsSdkEmitContext) {}
}
