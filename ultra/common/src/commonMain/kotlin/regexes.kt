@file:Suppress("Detekt:MaximumLineLength", "Detekt:MaxLineLength")

package io.peekandpoke.ultra.common

/**
 * Regex that matches a URL with a protocol scheme (e.g. `https://example.com`).
 *
 * Case-insensitive and UNANCHORED — `find`/`containsMatchIn` will happily match a URL embedded in a
 * larger text. Use `matches` (as [isUrlWithProtocol] does) when the whole string must be a URL.
 */
val UrlWithProtocolRegex = Regex(
    pattern = "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)",
    options = setOf(RegexOption.IGNORE_CASE),
)

/**
 * Regex that matches a slug / DNS label: lowercase ASCII letters and digits with hyphens allowed
 * only between them (no leading or trailing hyphen), length 1..63.
 *
 * Anchored, unlike [UrlWithProtocolRegex] and [EmailRegex].
 */
val SlugRegex = Regex("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$")

/**
 * Regex that matches an email address per RFC 5322 (simplified).
 *
 * Case-insensitive and UNANCHORED — use `matches`, as [isEmail] does, to require a full match.
 *
 * BOUND THE INPUT LENGTH BEFORE MATCHING. The dot-separated repetitions compile to nested loops that
 * the JVM engine walks recursively, so an input of a few thousand characters (~2000 `x.` segments)
 * overflows the stack with a `StackOverflowError` — an `Error`, which a `catch (e: Exception)` will
 * not stop. `EmailAddress` caps at 254 characters before it ever gets here; direct callers must do
 * the same.
 */
@Suppress("RegExpRedundantEscape")
val EmailRegex = Regex(
    pattern = "(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])",
    options = setOf(RegexOption.IGNORE_CASE)
)
