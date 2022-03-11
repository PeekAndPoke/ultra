package de.peekandpoke.ultra.kontainer

/**
 * Creates a kontainer blueprint
 */
fun kontainer(builder: KontainerBuilder.() -> Unit): KontainerBlueprint =
    KontainerBuilder(builder).build()

/**
 * Creates a kontainer module
 */
fun module(builder: KontainerBuilder.() -> Unit) = KontainerModule(builder)

/**
 * Creates a parameterized kontainer module
 */
fun <P> module(builder: KontainerBuilder.(P) -> Unit) = ParameterizedKontainerModule(builder)

/**
 * Creates a parameterized kontainer module with two parameters
 */
fun <P1, P2> module(builder: KontainerBuilder.(P1, P2) -> Unit) = ParameterizedKontainerModule2(builder)

/**
 * Creates a parameterized kontainer module with three parameters
 */
fun <P1, P2, P3> module(builder: KontainerBuilder.(P1, P2, P3) -> Unit) = ParameterizedKontainerModule3(builder)

/**
 * Kontainer module
 */
class KontainerModule(private val module: KontainerBuilder.() -> Unit) {
    fun apply(builder: KontainerBuilder) {
        builder.module()
    }
}

/**
 * Parameterized Kontainer module
 */
class ParameterizedKontainerModule<P>(private val module: KontainerBuilder.(P) -> Unit) {
    fun apply(builder: KontainerBuilder, param: P) {
        builder.module(param)
    }
}

/**
 * Parameterized Kontainer module
 */
class ParameterizedKontainerModule2<P1, P2>(private val module: KontainerBuilder.(P1, P2) -> Unit) {
    fun apply(builder: KontainerBuilder, p1: P1, p2: P2) {
        builder.module(p1, p2)
    }
}

/**
 * Parameterized Kontainer module
 */
class ParameterizedKontainerModule3<P1, P2, P3>(private val module: KontainerBuilder.(P1, P2, P3) -> Unit) {
    fun apply(builder: KontainerBuilder, p1: P1, p2: P2, p3: P3) {
        builder.module(p1, p2, p3)
    }
}
