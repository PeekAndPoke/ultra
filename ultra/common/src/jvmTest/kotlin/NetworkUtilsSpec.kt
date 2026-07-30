package io.peekandpoke.ultra.common.network

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch

class NetworkUtilsSpec : StringSpec({

    "getNetworkFingerPrint is stable across calls" {
        // GlobalLocksCleanupOnAppStarting reclaims a server's own locks by this id, so an unstable
        // fingerprint means a restarted server cannot recognise them and they wait for the sweep
        val first = NetworkUtils.getNetworkFingerPrint()

        repeat(20) {
            NetworkUtils.getNetworkFingerPrint() shouldBe first
        }
    }

    "getNetworkFingerPrint yields an md5 hex string, or the known stand-in" {
        val fingerprint = NetworkUtils.getNetworkFingerPrint()

        if (fingerprint != NetworkUtils.UNKNOWN_FINGERPRINT) {
            fingerprint shouldMatch "[0-9a-f]{32}"
        }
    }

    "the fallback is a constant, not something per-call" {
        // a time-based fallback used to hand out a new identity on every call
        NetworkUtils.UNKNOWN_FINGERPRINT shouldBe NetworkUtils.UNKNOWN_FINGERPRINT
        NetworkUtils.UNKNOWN_FINGERPRINT shouldNotBe ""
    }

    "getHostNameOrDefault returns something usable" {
        NetworkUtils.getHostNameOrDefault() shouldNotBe ""
        NetworkUtils.getHostNameOrDefault("fallback") shouldNotBe ""
    }
})
