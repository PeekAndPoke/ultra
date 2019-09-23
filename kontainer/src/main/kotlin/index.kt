package de.peekandpoke.ultra.kontainer

fun kontainer(builder: KontainerBuilder.() -> Unit): KontainerBlueprint = KontainerBuilder(builder).build()
