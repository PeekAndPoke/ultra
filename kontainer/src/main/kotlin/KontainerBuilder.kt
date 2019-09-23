package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

class KontainerBuilder(builder: KontainerBuilder.() -> Unit) {

    private val config = mutableMapOf<String, Any>()

    internal val classes = mutableMapOf<KClass<*>, InjectionType>()

    init {
        builder(this)
    }

    fun build(): KontainerBlueprint = KontainerBlueprint(config.toMap(), classes)

    // adding services ///////////////////////////////////////////////////////////////////////////////////////

    fun <T : Any> add(def: KClass<T>, type: InjectionType) = apply { classes[def] = type }

    inline fun <reified T : Any> singleton() = add(T::class, InjectionType.Singleton)

    inline fun <reified T : Any> dynamic() = add(T::class, InjectionType.Dynamic)

    // adding config values //////////////////////////////////////////////////////////////////////////////////

    fun config(id: String, value: Int) = apply { config[id] = value }

    fun config(id: String, value: Long) = apply { config[id] = value }

    fun config(id: String, value: Float) = apply { config[id] = value }

    fun config(id: String, value: Double) = apply { config[id] = value }

    fun config(id: String, value: String) = apply { config[id] = value }

    fun config(id: String, value: Boolean) = apply { config[id] = value }

}
