package io.peekandpoke.ultra.kontainer

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

/**
 * Service providers create instances of services
 */
interface ServiceProvider {

    /**
     * Provides a [ServiceProvider]
     */
    interface Provider {

        companion object {
            /** An instance handed in at [KontainerBlueprint.create], replacing a dynamic singleton */
            fun forOverride(definition: ServiceDefinition, instance: Any): Provider {
                return OverrideProvider(definition = definition, instance = instance)
            }

            /** A static singleton: one provider, so one instance, for all kontainers of the blueprint */
            fun forGlobalSingleton(definition: ServiceDefinition): Provider {
                return ForGlobalSingleton(definition = definition)
            }

            /** A dynamic or semi dynamic singleton: one provider, so one instance, per kontainer */
            fun forDynamicSingleton(kind: ServiceKind, definition: ServiceDefinition): Provider {
                require(kind != ServiceKind.Static) {
                    "A per-kontainer singleton is Dynamic or SemiDynamic, not Static"
                }

                return ForDynamicSingleton(kind = kind, definition = definition)
            }
        }

        private class OverrideProvider(definition: ServiceDefinition, instance: Any) : Provider {

            private val provider = ForOverride(definition = definition, instance = instance)

            override fun provide(): ForOverride {
                return provider
            }
        }

        private class ForGlobalSingleton(definition: ServiceDefinition) : Provider {

            private val provider = ForSingleton(kind = ServiceKind.Static, definition = definition)

            override fun provide(): ForSingleton {
                return provider
            }
        }

        private class ForDynamicSingleton(
            private val kind: ServiceKind,
            private val definition: ServiceDefinition,
        ) : Provider {

            override fun provide(): ForSingleton {
                return ForSingleton(kind = kind, definition = definition)
            }
        }

        fun provide(): ServiceProvider
    }

    /**
     * What a provider created: how many instances, and when the first and the last one were made.
     *
     * Both times are null exactly when [count] is 0. No reference to an instance is kept.
     */
    data class Created(
        val count: Long,
        val firstAt: Instant?,
        val lastAt: Instant?,
    ) {
        companion object {
            /** Nothing created yet */
            val none = Created(count = 0, firstAt = null, lastAt = null)

            /** One instance, created [at] */
            fun once(at: Instant) = Created(count = 1, firstAt = at, lastAt = at)
        }
    }

    /**
     * What the service is: singleton or prototype.
     */
    val type: ServiceType

    /**
     * What the service is bound to, as derived by the blueprint.
     */
    val kind: ServiceKind

    /**
     * True when the instance was handed in at [KontainerBlueprint.create], replacing a dynamic singleton.
     */
    val overridden: Boolean

    /**
     * What the provider created so far.
     */
    val created: Created

    /**
     * The definition of the service.
     */
    val definition: ServiceDefinition

    /**
     * Provides the service instance
     */
    fun provide(kontainer: Kontainer, context: InjectionContext): Any

    /**
     * Validates that a service can be provided.
     *
     * When all is well an empty list is returned.
     * Otherwise, a list of error strings is returned.
     */
    fun validate(kontainer: Kontainer): List<String>

    /**
     * Provides an instance handed in at [KontainerBlueprint.create], replacing a dynamic singleton
     */
    data class ForOverride(
        override val definition: ServiceDefinition,
        private val instance: Any,
    ) : ServiceProvider {

        /** Always a singleton */
        override val type: ServiceType = ServiceType.Singleton

        /** Always dynamic: only dynamic singletons can be overridden */
        override val kind: ServiceKind = ServiceKind.Dynamic

        /** Always overridden */
        override val overridden: Boolean = true

        /**
         * Always one instance, made when the provider was, i.e. when the kontainer was created
         */
        override val created: Created = Created.once(Instant.now())

        /**
         * Simply returns the [instance]
         */
        override fun provide(kontainer: Kontainer, context: InjectionContext) = instance

        /**
         * Always valid
         */
        override fun validate(kontainer: Kontainer) = listOf<String>()
    }

    /**
     * Provides a singleton: a global one for [ServiceKind.Static], otherwise one per kontainer
     */
    data class ForSingleton(
        override val kind: ServiceKind,
        override val definition: ServiceDefinition,
    ) : ServiceProvider {

        /** Always a singleton */
        override val type: ServiceType = ServiceType.Singleton

        /** Never handed in at create */
        override val overridden: Boolean = false

        /** When the instance was created; written before [instance], so it is set whenever [instance] is */
        @Volatile
        private var createdAt: Instant? = null

        @Volatile
        private var instance: Any? = null

        /**
         * One instance once it was created, none before
         */
        override val created: Created
            get() = createdAt?.let { Created.once(it) } ?: Created.none

        /**
         * Get or create the instance of the service
         */
        override fun provide(kontainer: Kontainer, context: InjectionContext): Any {
            return instance ?: synchronized(this) {
                instance ?: create(creatingKontainer(kontainer), context).apply {
                    createdAt = Instant.now()
                    instance = this
                }
            }
        }

        /**
         * Validates that all parameters can be provided
         */
        override fun validate(kontainer: Kontainer) = definition.producer.params.flatMap {
            it.validate(kontainer)
        }

        /**
         * The kontainer the instance is created in: the blueprint-wide one for a global singleton, so its parameters
         * do not bind to the kontainer that asked first; the asking one otherwise.
         */
        private fun creatingKontainer(kontainer: Kontainer): Kontainer = when (kind) {
            ServiceKind.Static -> kontainer.blueprint.globalKontainer
            ServiceKind.SemiDynamic, ServiceKind.Dynamic -> kontainer
        }

        /**
         * Creates a new instance
         */
        private fun create(kontainer: Kontainer, context: InjectionContext): Any {

            val nextCtx = context.next(definition.serviceCls)

            return definition.producer.creator(
                kontainer,
                definition.producer.params.map {
                    it.provide(
                        kontainer = kontainer,
                        context = nextCtx
                    )
                }.toTypedArray()
            )
        }
    }

    /**
     * Provides a prototype service
     *
     * Each call to [provide] will create a new instance.
     */
    data class ForPrototype(
        override val kind: ServiceKind,
        override val definition: ServiceDefinition,
    ) : ServiceProvider {

        /** One provider per kontainer, whatever the [kind] */
        class Provider(
            private val kind: ServiceKind,
            private val definition: ServiceDefinition,
        ) : ServiceProvider.Provider {

            override fun provide(): ForPrototype {
                return ForPrototype(kind = kind, definition = definition)
            }
        }

        /** Always a prototype */
        override val type: ServiceType = ServiceType.Prototype

        /** Never handed in at create */
        override val overridden: Boolean = false

        /** Updated without a lock: concurrent creations run in parallel and are all counted */
        private val counter = AtomicReference(Created.none)

        /**
         * How many instances this provider created, the earliest and the latest creation time
         */
        override val created: Created get() = counter.get()

        /**
         * Creates a new instance of the service
         */
        override fun provide(kontainer: Kontainer, context: InjectionContext): Any {

            val instance = create(kontainer, context)
            val now = Instant.now()

            counter.updateAndGet { current ->
                Created(
                    count = current.count + 1,
                    firstAt = current.firstAt?.let { minOf(it, now) } ?: now,
                    lastAt = current.lastAt?.let { maxOf(it, now) } ?: now,
                )
            }

            return instance
        }

        /**
         * Validates that all parameters can be provided
         */
        override fun validate(kontainer: Kontainer) = definition.producer.params.flatMap {
            it.validate(kontainer)
        }

        /**
         * Creates a new instance
         */
        private fun create(kontainer: Kontainer, context: InjectionContext): Any {

            val nextContext = context.next(definition.serviceCls)

            return definition.producer.creator(
                kontainer,
                definition.producer.params.map {
                    it.provide(
                        kontainer = kontainer,
                        context = nextContext,
                    )
                }.toTypedArray()
            )
        }
    }
}
