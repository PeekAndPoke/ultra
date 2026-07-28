package io.peekandpoke.funktor.messaging.templates

import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.ultra.i18n.Locale

/**
 * Builds ONE email, in code.
 *
 * Rendered in Kotlin rather than from a text template on purpose. A template engine substituting
 * `{{name}}` placeholders has one failure mode that no amount of care removes: a value that is
 * renamed, misspelled or forgotten ships a literal `{{name}}` to a real recipient, and nothing but a
 * test that happens to assert on that exact line will ever notice. Here [PARAMS] is a type — a
 * missing value is a compile error.
 *
 * A template produces the [Email] and nothing downstream of it. Delivery, storage policy and
 * anonymization belong to the caller, which is what keeps an app-supplied template from choosing
 * whether its own live tokens get persisted.
 */
interface EmailTemplate<PARAMS> {
    /**
     * Renders in the first locale of [preferred] this template has a rendering for, most-specific
     * first, falling back to one the template guarantees.
     */
    fun render(params: PARAMS, preferred: List<Locale>): Email
}

/**
 * An [EmailTemplate] that keeps one rendering function per locale and walks a fallback chain.
 *
 * One walker for every template, rather than a chain hand-rolled per email — three copies of this
 * logic would drift, and the one that drifts is the one nobody notices because a wrong-language email
 * still looks like a working email.
 *
 * **The whole email is chosen at once.** [renderers] maps a locale to a function producing the entire
 * message, so a subject can never end up in one language and a body in another. Assembling an email
 * out of per-key fragments is the thing this design exists to prevent.
 */
abstract class LocalizedEmailTemplate<PARAMS> : EmailTemplate<PARAMS> {

    /**
     * The locale this template GUARANTEES a rendering for — the end of every chain.
     *
     * It, or its base language, must be a key of [renderers]; [validate] is what enforces that.
     */
    abstract val fallbackLocale: Locale

    /**
     * One whole-email rendering function per locale.
     *
     * A renderer is HANDED the locale it was selected for rather than naming its own. The map key and
     * the rendering would otherwise be two independent statements of the same fact, free to disagree
     * — copy `renderDe` to `renderFr`, translate the strings, forget the locale literal, and French
     * copy ships inside `<html lang="de">` under a German footer. That is exactly the mixed-language
     * mail this design exists to make impossible, so it must not be reachable by a typo.
     *
     * The locale passed is the one that MATCHED, not the one requested: a `de-CH` request resolving
     * to the `de` rendering hands the renderer `de`, which is what the layout should declare.
     */
    abstract val renderers: Map<Locale, (PARAMS, Locale) -> Email>

    /**
     * Fails loudly when [renderers] can serve neither [fallbackLocale] nor its base language.
     *
     * Accepting the base is not laxity — it is the same widening [rendererFor] performs, and checking
     * a stricter rule than the lookup uses would reject a template declaring `de-CH` while shipping a
     * perfectly serviceable `de` rendering.
     *
     * Called at WIRING time (see `AuthEmailTemplates`) rather than per email, so a template that
     * cannot render its own guaranteed locale surfaces when the realm is constructed rather than on
     * the first sign-up that happens to reach it.
     */
    fun validate() {
        require(renderers.containsKey(fallbackLocale) || renderers.containsKey(fallbackLocale.base)) {
            "${this::class.simpleName} declares fallbackLocale '${fallbackLocale.tag}' but has no " +
                    "renderer for it (has: ${renderers.keys.joinToString { it.tag }})"
        }
    }

    override fun render(params: PARAMS, preferred: List<Locale>): Email {
        val (locale, renderer) = rendererFor(preferred)

        return renderer(params, locale)
    }

    /**
     * The locale [preferred] resolves to, and the rendering function for it.
     *
     * Each entry contributes itself AND its base language before the next is tried, so `de-CH` finds
     * a `de` rendering ahead of the next preference rather than skipping past it. [fallbackLocale] is
     * always appended, so a validated template cannot fail to find one.
     *
     * Widening goes region -> base ONLY. A template shipping only `de-CH` does not serve a plain `de`
     * preference; key it by `de` if it should.
     *
     * Returns the locale as well as the function because the renderer needs it — see [renderers].
     */
    fun rendererFor(preferred: List<Locale>): Pair<Locale, (PARAMS, Locale) -> Email> {
        val chain = (preferred + fallbackLocale).flatMap { listOf(it, it.base) }.distinct()

        chain.forEach { locale -> renderers[locale]?.let { return locale to it } }

        // Unreachable once validate() has run — kept as a real error rather than !! so that a
        // template constructed outside the wiring path still fails with something readable.
        throw IllegalStateException(
            "${this::class.simpleName} has no renderer for its fallbackLocale '${fallbackLocale.tag}'"
        )
    }
}
