package io.peekandpoke.kraft.i18n

import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.i18n.I18n
import io.peekandpoke.ultra.i18n.I18nFormat
import io.peekandpoke.ultra.i18n.I18nTranslate
import io.peekandpoke.ultra.i18n.Locale
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe
import io.peekandpoke.ultra.streams.ops.map
import io.peekandpoke.ultra.streams.ops.persistInLocalStorage
import kotlinx.browser.window
import kotlinx.serialization.builtins.serializer

/**
 * App-wide i18n state for kraft — the mirror of `ResponsiveController`. The chosen locale is a
 * persisted stream; the current [I18n] snapshot is derived from it via [I18n.withLocale] whenever the
 * language changes. Register it on the app (`kraftApp { i18n(...) }`) and consume it in components via
 * `by Translations` / `by Formatting`, which re-render on switch.
 */
class I18nController private constructor(
    private val base: I18n,
    private val chosenTag: StreamSource<String>,
) : Stream<I18n> {

    companion object {
        val key = TypedKey<I18nController>("kraft.i18n")

        /**
         * A localStorage-persisted controller. kraft's built-in form catalog is ALWAYS installed
         * (lowest precedence), so form errors translate even if the app forgets; the app's [catalogs]
         * install afterwards and win on shared keys (D3). Boot language precedence: the stored value
         * (via [persistInLocalStorage]) wins, otherwise [initialLang] (defaults to [browserLang]).
         */
        fun create(
            locale: Locale,
            fallback: Locale,
            initialLang: String = browserLang(fallback.tag),
            storageKey: String = "kraft.i18n.lang",
            catalogs: I18n.Builder.() -> Unit = {},
        ): I18nController = I18nController(
            base = I18n(locale, fallback) { installKraftForms(); catalogs() },
            chosenTag = StreamSource(initialLang).persistInLocalStorage(storageKey, String.serializer()),
        )

        /** A non-persisted controller (tests, SSR); kraft's form catalog is always installed. */
        fun inMemory(
            locale: Locale,
            fallback: Locale = locale,
            initialLang: String = locale.tag,
            catalogs: I18n.Builder.() -> Unit = {},
        ): I18nController = I18nController(
            base = I18n(locale, fallback) { installKraftForms(); catalogs() },
            chosenTag = StreamSource(initialLang),
        )

        /** The framework default: English with only kraft's form catalog. Overridden by `kraftApp { i18n(...) }`. */
        fun default(): I18nController = inMemory(Locale("en"), Locale("en"))

        /** The browser's preferred language tag, or [fallback] when unavailable. */
        fun browserLang(fallback: String): String =
            window.navigator.language.takeIf { it.isNotBlank() } ?: fallback
    }

    /** The current [I18n], re-derived whenever the chosen locale changes. */
    private val i18nStream: Stream<I18n> = chosenTag.map { base.withLocale(Locale.parse(it)) }

    /** The open translation surface, as a stream — what `by Translations` subscribes to. */
    val translateStream: Stream<I18nTranslate> = i18nStream.map { it.translate }

    /** The closed formatting surface, as a stream — what `by Formatting` subscribes to. */
    val formatStream: Stream<I18nFormat> = i18nStream.map { it.format }

    override fun invoke(): I18n = i18nStream()

    override fun subscribeToStream(sub: (I18n) -> Unit): Unsubscribe = i18nStream.subscribeToStream(sub)

    val locale: Locale get() = i18nStream().locale

    /**
     * Switches the active language. `suspend` by design (D4): fast today, it is the seam where a
     * language's catalog would be fetched before the new snapshot is emitted once lazy loading lands.
     */
    suspend fun setLang(locale: Locale) {
        chosenTag(locale.tag)
    }
}
