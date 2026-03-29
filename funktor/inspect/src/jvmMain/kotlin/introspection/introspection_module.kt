package io.peekandpoke.funktor.inspect.introspection

import io.peekandpoke.funktor.inspect.introspection.api.IntrospectionApiFeature
import io.peekandpoke.funktor.inspect.introspection.services.ApiAccessDescriptor
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module

fun KontainerBuilder.funktorIntrospection() = module(Funktor_Introspection)

val Funktor_Introspection = module {
    singleton(IntrospectionApiFeature::class)
    singleton(ApiAccessDescriptor::class)
}
