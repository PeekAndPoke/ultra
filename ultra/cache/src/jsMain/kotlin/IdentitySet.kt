package io.peekandpoke.ultra.cache

/**
 * Backed by a JS `Set`, constant time: objects by identity. Strings are JS primitives and compare by
 * value, so two equal strings count once - there is no string identity on JS.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual class IdentitySet actual constructor() {
    private val seen: dynamic = js("new Set()")

    actual fun add(obj: Any): Boolean {
        if (seen.has(obj) as Boolean) {
            return false
        }

        seen.add(obj)

        return true
    }
}
