package io.peekandpoke.ultra.cache

/**
 * Keeps [cache] reachable up to the call.
 *
 * The processing loop holds its cache only weakly and stops once the cache is collected. A test that
 * stops using its cache and then waits for the loop - an expiry, a refresh, a statistic - fails when a
 * collection hits that wait, and a test asserting that something does NOT happen passes vacuously.
 * Call this after the last wait.
 */
fun keepReachable(cache: FastCache<*, *>) {
    check(cache.size >= 0)
}
