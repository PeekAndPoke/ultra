package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class FastCacheBuilderSpec : StringSpec() {
    init {
        "builder with no behaviours creates cache with empty behaviours list" {
            val cache = fastCache<String, String>()

            cache.behaviours.shouldBeEmpty()
        }

        "builder with expireAfterAccess adds ExpireAfterAccessBehaviour" {
            val cache = fastCache<String, String> {
                expireAfterAccess(5.seconds)
            }

            cache.behaviours shouldHaveSize 1
            cache.behaviours[0].shouldBeInstanceOf<FastCache.ExpireAfterAccessBehaviour<String, String>>()
        }

        "builder with maxEntries adds MaxEntriesBehaviour" {
            val cache = fastCache<String, String> {
                maxEntries(100)
            }

            cache.behaviours shouldHaveSize 1
            cache.behaviours[0].shouldBeInstanceOf<FastCache.MaxEntriesBehaviour<String, String>>()
        }

        "builder with maxMemoryUsage adds MaxMemoryUsageBehaviour" {
            val cache = fastCache<String, String> {
                maxMemoryUsage(1024)
            }

            cache.behaviours shouldHaveSize 1
            cache.behaviours[0].shouldBeInstanceOf<FastCache.MaxMemoryUsageBehaviour<String, String>>()
        }

        "builder with multiple behaviours preserves order" {
            val cache = fastCache<String, String> {
                expireAfterAccess(1.seconds)
                maxEntries(10)
                maxMemoryUsage(512)
            }

            cache.behaviours shouldHaveSize 3
            cache.behaviours[0].shouldBeInstanceOf<FastCache.ExpireAfterAccessBehaviour<String, String>>()
            cache.behaviours[1].shouldBeInstanceOf<FastCache.MaxEntriesBehaviour<String, String>>()
            cache.behaviours[2].shouldBeInstanceOf<FastCache.MaxMemoryUsageBehaviour<String, String>>()
        }

        "builder loopDelay is applied to cache" {
            val delay = 200.milliseconds

            val cache = fastCache<String, String>(loopDelay = delay)

            cache.loopDelay shouldBe delay
        }

        "default loopDelay matches FastCache.defaultLoopDelay" {
            val cache = fastCache<String, String>()

            cache.loopDelay shouldBe FastCache.defaultLoopDelay
        }
    }
}
