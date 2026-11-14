package io.peekandpoke.ultra.cache

/**
 * The bound every spec of this module sets on each of its tests, instead of kotest's 10 minutes.
 *
 * Far above the longest test here; it exists so that a hang - a loop that died, a gate never opened -
 * fails the test within a minute instead of holding up the whole build.
 */
const val TEST_TIMEOUT_MS = 60_000L
