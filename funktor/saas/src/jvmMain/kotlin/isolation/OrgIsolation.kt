package io.peekandpoke.funktor.saas.isolation

import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.ultra.vault.Ref
import io.peekandpoke.ultra.vault.Stored

/**
 * Marks a domain entity that is OWNED by an organisation — it carries a reference [org] to its
 * owning [Organisation].
 *
 * ## Enforced ONLY when the saas funktor module is installed
 *
 * `OrgAware` / [OrgAwareParam] are plain interfaces on the saas classpath, but the checks that make
 * them binding — `OrgIsolationBootCheck` + `OrgIsolationGuard` — are registered by the `funktor(saas
 * = {…})` module. Any app using these interfaces MUST install funktor saas.
 *
 * In practice a canonical [OrgAwareParam] route CANNOT function without saas anyway: its
 * `org: Stored<Organisation>` param needs the `Organisation` repository that saas registers, so
 * without it the `{org}` url segment fails to convert (and `ConverterCompatBootCheck` flags it at
 * boot). The only genuinely silent gap is the unusual shape that loads an `OrgAware` entity WITHOUT
 * an [OrgAwareParam] `org` param — covered by the entity-`OrgAware` linter follow-up.
 *
 * Marking an entity `OrgAware` is the developer's OPT-IN (a lint/boot rule to flag org-owned entities
 * that forgot it is future scope). Once marked, any route resolving the entity is boot-forced to be
 * [OrgAwareParam] (see `OrgIsolationBootCheck`), and every such entity is checked against the
 * request's org at runtime (see `OrgIsolationGuard`).
 */
interface OrgAware {
    /**
     * Reference to the owning organisation.
     *
     * CONTRACT: [org] MUST carry the org's CANONICAL `_id` (the collection-qualified id, e.g.
     * `organisation/acme`), because `OrgIsolationGuard` compares it via `hasSameIdAs` (which matches
     * on `_id`) against the loaded `param.org._id`. A persisted `Ref<Organisation>` satisfies this
     * automatically; if you construct it, use the org's `_id` — NOT a bare `_key`. A bare `_key`
     * makes EVERY same-org request 404 (fail-closed), so this is an availability landmine, not an
     * IDOR — but get it right. `_id`/`_key` are available without resolving the ref.
     */
    val org: Ref<Organisation>
}

/**
 * Marks a route-params object whose request is scoped to a single [Organisation], resolved from the
 * URL into [org]. This is the TENANT BOUNDARY: `OrgIsolationGuard` binds the caller's SELECTED
 * session org to [org] (`hasOrganisation`) and checks every [OrgAware] entity in the params belongs
 * to it (`entity.org hasSameIdAs org`); any failure answers 404 (hidden as not-found).
 *
 * Binds against the SELECTED org, never `accessibleOrgs` (the login picker's candidate set, marked
 * "Non-authz"): under the one-active-org-per-session model the session's permission slice is the
 * selected org's only, so a multi-org user who selected org A must not reach org B without
 * re-selecting.
 *
 * Enforced only when the saas module is installed — see [OrgAware]. Covers only entities resolved as
 * `Stored`/`Storable<X>` URL params (the ones the incoming converter loads via `findById`); an
 * entity a handler loads itself from a raw `String` path param, or one carried in the request BODY,
 * is NOT covered and stays the handler's responsibility.
 */
interface OrgAwareParam {
    /** The organisation the request addresses, resolved from the URL. */
    val org: Stored<Organisation>
}
