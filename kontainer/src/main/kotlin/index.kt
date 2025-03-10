package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * Creates a kontainer blueprint
 */
@DslMarker
annotation class KontainerDsl

@DslMarker
annotation class KontainerDslModule

@DslMarker
annotation class KontainerDslSingleton

@DslMarker
annotation class KontainerDslDynamic

@DslMarker
annotation class KontainerDslPrototype

/** Creates a kontainer blueprint */
@KontainerDsl
fun kontainer(builder: KontainerBuilder.() -> Unit): KontainerBlueprint =
    KontainerBuilder(builder).build()

/** Creates a kontainer module */
@KontainerDslModule
fun module(builder: KontainerBuilder.() -> Unit) =
    KontainerModule(builder)

/** Creates a parameterized kontainer module */
@KontainerDslModule
fun <P> module(builder: KontainerBuilder.(P) -> Unit) =
    ParameterizedKontainerModule(builder)

/** Creates a parameterized kontainer module with two parameters */
@KontainerDslModule
fun <P1, P2> module(builder: KontainerBuilder.(P1, P2) -> Unit) =
    ParameterizedKontainerModule2(builder)

/** Creates a parameterized kontainer module with three parameters */
@KontainerDslModule
fun <P1, P2, P3> module(builder: KontainerBuilder.(P1, P2, P3) -> Unit) =
    ParameterizedKontainerModule3(builder)

/** Creates a parameterized kontainer module with four parameters */
@KontainerDslModule
fun <P1, P2, P3, P4> module(builder: KontainerBuilder.(P1, P2, P3, P4) -> Unit) =
    ParameterizedKontainerModule4(builder)

/** Creates a parameterized kontainer module with five parameters */
@KontainerDslModule
fun <P1, P2, P3, P4, P5> module(builder: KontainerBuilder.(P1, P2, P3, P4, P5) -> Unit) =
    ParameterizedKontainerModule5(builder)

/**
 * Get name
 */
internal fun KClass<*>.getName() = this.qualifiedName ?: this.simpleName ?: "<unknown>"

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
@KontainerDslModule
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

/**
 * Parameterized Kontainer module
 */
class ParameterizedKontainerModule4<P1, P2, P3, P4>(private val module: KontainerBuilder.(P1, P2, P3, P4) -> Unit) {
    fun apply(builder: KontainerBuilder, p1: P1, p2: P2, p3: P3, p4: P4) {
        builder.module(p1, p2, p3, p4)
    }
}

/**
 * Parameterized Kontainer module
 */
class ParameterizedKontainerModule5<P1, P2, P3, P4, P5>(private val module: KontainerBuilder.(P1, P2, P3, P4, P5) -> Unit) {
    fun apply(builder: KontainerBuilder, p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) {
        builder.module(p1, p2, p3, p4, p5)
    }
}
