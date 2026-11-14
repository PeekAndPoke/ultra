package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/** JS specifics of the default estimator. */
class ObjectSizeEstimatorJsSpec : StringSpec({

    timeout = TEST_TIMEOUT_MS

    "every JS number is charged 8 bytes, a double's size" {
        val estimator = ObjectSizeEstimator()

        estimator.estimate(1) shouldBe 8L
        estimator.estimate(1.5) shouldBe 8L
        estimator.estimate(1.toByte()) shouldBe 8L
    }

    "a property whose getter throws is charged flat, the estimate does not throw" {
        val obj: dynamic = js("({})")
        js("Object.defineProperty(obj, 'bad', { enumerable: true, get: function() { throw new Error('getter failed'); } })")
        obj.good = "fine"

        // header and two references; the failing property charged flat (header and two references);
        // "fine" as a string - not the flat charge of a walk that gave up on the whole object
        ObjectSizeEstimator().estimate(obj) shouldBe (16L + 2 * 8L) + (16L + 2 * 8L) + (16L + 8L + 24L + 2 * 4L)
    }

    "a getter throwing something that is not an Error is charged flat as well" {
        val obj: dynamic = js(
            "(function() { var o = { good: 'fine' }; " +
                "Object.defineProperty(o, 'bad', { enumerable: true, get: function() { throw 'not an Error'; } }); " +
                "return o; })()"
        )

        ObjectSizeEstimator().estimate(obj) shouldBe (16L + 2 * 8L) + (16L + 2 * 8L) + (16L + 8L + 24L + 2 * 4L)
    }

    "typed arrays, an ArrayBuffer and a DataView are sized from their byte length, a Blob from its size" {
        val estimator = ObjectSizeEstimator()

        estimator.estimate(js("new Uint8Array(1000)")) shouldBe 24L + 1_000L
        estimator.estimate(js("new Float32Array(10)")) shouldBe 24L + 40L
        estimator.estimate(js("new ArrayBuffer(1000000)")) shouldBe 24L + 1_000_000L
        estimator.estimate(js("new DataView(new ArrayBuffer(100))")) shouldBe 24L + 100L
        estimator.estimate(js("new Blob(['abc'])")) shouldBe 16L + 3L
    }

    "a JS Map and a JS Set are charged an empty object: their entries are not seen" {
        val estimator = ObjectSizeEstimator()

        estimator.estimate(js("new Map([['a', 'b'], ['c', 'd']])")) shouldBe 16L
        estimator.estimate(js("new Set(['a', 'b'])")) shouldBe 16L
    }

    "two equal strings count once: JS strings have no identity" {
        val a = "x".repeat(10)
        val b = "x".repeat(10)

        // the list (16 + 2 * 8) and one string of 10 chars (16 + 8 + 24 + 20)
        ObjectSizeEstimator().estimate(listOf(a, b)) shouldBe 32L + 68L
    }

    "an object with many own properties has only a sample of them read" {
        val made: dynamic = js(
            "(function() { var c = { n: 0 }; var o = {}; for (var i = 0; i < 1000; i++) { " +
                "Object.defineProperty(o, 'p' + i, { enumerable: true, get: function() { c.n++; return 'v'; } }); " +
                "} return { o: o, c: c }; })()"
        )

        val estimate = ObjectSizeEstimator().estimate(made.o)
        val reads = made.c.n as Int

        (reads <= 64) shouldBe true
        // the header and a reference per property, at the least
        (estimate >= 16L + 1_000 * 8L) shouldBe true
    }

    "a throwing getter is counted as a failure" {
        val obj: dynamic = js(
            "(function() { var o = { good: 'fine' }; " +
                "Object.defineProperty(o, 'bad', { enumerable: true, get: function() { throw new Error('no'); } }); " +
                "Object.defineProperty(o, 'worse', { enumerable: true, get: function() { throw 'no'; } }); " +
                "return o; })()"
        )

        val counted = ObjectSizeEstimatorImpl().estimateCounted(obj)

        counted.failures shouldBe 2
        // header and three references, two properties flat, "fine"
        counted.bytes shouldBe (16L + 3 * 8L) + 2 * 32L + 56L
    }

    "a proxy whose key listing throws a non-Error value is charged flat and counted" {
        val proxy: dynamic = js("new Proxy({}, { ownKeys: function() { throw 'no keys'; } })")

        val counted = ObjectSizeEstimatorImpl().estimateCounted(proxy)

        counted.bytes shouldBe 32L
        counted.failures shouldBe 1
        counted.failureClass shouldBe "ThrownValue"

        // inside a list: the proxy alone is charged flat; the list (16 + 2 * 8) and "ok" (52) stand
        val inList = ObjectSizeEstimatorImpl().estimateCounted(listOf<Any?>("ok", proxy))

        inList.bytes shouldBe 32L + 52L + 32L
        inList.failures shouldBe 1
    }

    "a revoked proxy is charged flat and counted; the elements beside it stand" {
        fun revoked(): dynamic = js("(function() { var r = Proxy.revocable({}, {}); r.revoke(); return r.proxy; })()")

        val alone = ObjectSizeEstimatorImpl().estimateCounted(revoked())

        alone.bytes shouldBe 32L
        alone.failures shouldBe 1
        alone.failureClass shouldBe "TypeError"

        val inList = ObjectSizeEstimatorImpl().estimateCounted(listOf<Any?>("ok", revoked(), "after"))

        // the list (16 + 3 * 8), "ok" (52), the proxy flat (32), "after" (58)
        inList.bytes shouldBe 40L + 52L + 32L + 58L
        inList.failures shouldBe 1
    }
})
