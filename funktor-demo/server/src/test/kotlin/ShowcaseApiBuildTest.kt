package io.peekandpoke.funktor.demo.server

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.funktor.demo.server.api.showcase.ShowcaseApiFeature

/**
 * The showcase routes build without the app: a route's auth chain is checked at construction, and a failing one stops
 * the server at boot. The depot listings sit in the superuser group (ULTRA-00304): added to the public group, they
 * failed that check.
 */
class ShowcaseApiBuildTest : StringSpec({

    "the showcase routes build, the depot listings in the superuser group" {
        val feature = ShowcaseApiFeature()

        // The client's declared URIs, now served by the superuser group
        feature.clusterAdmin.getDepotRepos.pattern.pattern shouldContain "depot"
        feature.clusterAdmin.getDepotFiles.pattern.pattern shouldContain "depot"
    }
})
