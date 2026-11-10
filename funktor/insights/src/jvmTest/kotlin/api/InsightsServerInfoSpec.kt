package io.peekandpoke.funktor.insights.api

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.core.config.ktor.KtorConfig
import io.peekandpoke.funktor.core.model.AppVersion

/** What the list page header shows, from distinct values so a swapped field shows (ULTRA-00190). */
class InsightsServerInfoSpec : StringSpec({

    "the git version, its description, the application id and the environment, each from its own field" {
        val info = InsightsServerInfo.of(
            version = AppVersion(gitBranch = "main", gitRev = "abc1234", gitDesc = "v1.2.0-3-gabc1234"),
            ktor = KtorConfig(
                deployment = KtorConfig.Deployment(environment = "qa-eu"),
                application = KtorConfig.Application(id = "Shop | QA"),
            ),
        )

        info shouldBe InsightsServerInfo(
            git = "main-abc1234",
            gitDesc = "v1.2.0-3-gabc1234",
            applicationId = "Shop | QA",
            environment = "qa-eu",
        )
    }

    "a build without git info has an empty version, not n/a-n/a" {
        InsightsServerInfo.of(version = AppVersion(), ktor = KtorConfig()).git shouldBe ""
    }
})
