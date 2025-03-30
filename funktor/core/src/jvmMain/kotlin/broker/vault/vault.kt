package de.peekandpoke.ktorfx.core.broker.vault

import de.peekandpoke.ktorfx.core.broker.IncomingParamConverter
import de.peekandpoke.ktorfx.core.broker.OutgoingParamConverter
import de.peekandpoke.ultra.vault.Database
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.Stored
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Incoming param converter for Vault types
 *
 * - see [Stored]
 */
class IncomingVaultConverter(private val db: Database) : IncomingParamConverter {

    companion object {
        private val supportedClasses = listOf(Storable::class, Stored::class)
    }

    override fun canHandle(type: KType): Boolean {

        val inner = getStoredInner(type)

        return inner != null && db.hasRepositoryStoring(inner.java)
    }

    override suspend fun convert(value: String, type: KType): Stored<Any>? {
        return getStoredInner(type)?.let { inner ->
            db.getRepositoryStoring(inner.java).findById(value)
        }
    }

    private fun getStoredInner(type: KType): KClass<*>? {

        val isSupportedCls = type.classifier in supportedClasses

        return if (type.arguments.size == 1 && isSupportedCls) {
            type.arguments[0].type!!.classifier as? KClass<*>
        } else {
            null
        }
    }
}

/**
 * Outgoing param converter for Vault types
 *
 * - see [Stored]
 */
class OutgoingVaultConverter : OutgoingParamConverter {

    override fun canHandle(type: KType): Boolean {

        return type.arguments.size == 1 &&
                Storable::class.java.isAssignableFrom((type.classifier as KClass<*>).java)
    }

    override fun convert(value: Any, type: KType): String {
        return (value as? Storable<*>)?._key ?: ""
    }
}
