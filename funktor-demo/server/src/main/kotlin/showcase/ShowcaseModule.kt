package io.peekandpoke.funktor.demo.server.showcase

import io.peekandpoke.funktor.demo.server.api.showcase.ShowcaseApiFeature
import io.peekandpoke.ultra.kontainer.module

val ShowcaseModule = module {
    singleton(ShowcaseApiFeature::class)
}
