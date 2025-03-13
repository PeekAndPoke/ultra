package de.peekandpoke.ultra.common

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object RunSync {
    operator fun <R> invoke(lock: Any, block: () -> R): R
}
