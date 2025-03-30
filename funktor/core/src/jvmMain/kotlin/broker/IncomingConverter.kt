package de.peekandpoke.ktorfx.core.broker

import de.peekandpoke.ultra.common.Lookup
import de.peekandpoke.ultra.common.reflection.ReifiedKType
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.security.csrf.CsrfProtection
import io.ktor.http.*
import io.ktor.server.plugins.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.URLDecoder
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

/**
 * Converter for incoming url data
 */
class IncomingConverter(
    private val csrfProtection: CsrfProtection,
    private val lookUp: IncomingConverterLookup,
    private val converters: Lookup<IncomingParamConverter>,
) {
    suspend fun convert(routeParams: Parameters, queryParams: Parameters, type: TypeRef<*>): Any {
        return convert(routeParams, queryParams, type.type)
    }

    suspend fun convert(routeParams: Parameters, queryParams: Parameters, type: KType): Any {
        return convert(routeParams, queryParams, ReifiedKType(type))
    }

    suspend fun convert(routeParams: Parameters, queryParams: Parameters, type: ReifiedKType): Any {

        return when (type.cls) {

            Unit::class, Any::class -> Unit

            else -> coroutineScope {

                val csrf = routeParams["csrf"]

                // check if all non optional values are provided
                val callParams = type.ctorParams2Types
                    .map { it to (routeParams[it.first.name!!] ?: queryParams[it.first.name!!]) }
                    .filter { (_, v) -> v is String }
                    .map { (k, v) ->
                        async {
                            try {
                                // NOTICE: The 'decode' is a work-around for a bug in KTOR's test application calls.
                                //         In test application call the values are not decoded properly.
                                val converted = convert(decode(v as String), k.second)

                                if (converted == null && !k.first.type.isMarkedNullable) {
                                    throw IllegalArgumentException("Route-Parameter '${k.first.name}' cannot be null")
                                }

                                k.first to converted
                            } catch (e: IllegalArgumentException) {
                                // NOTICE: we ignore decoding errors if the parameter is optional
                                if (k.first.isOptional) {
                                    null
                                } else {
                                    throw NotFoundException()
                                }
                            }
                        }
                    }
                    .awaitAll()
                    .filterNotNull()
                    .toMap()

                val result = when (val ctor = type.cls.primaryConstructor) {

                    null -> type.cls.createInstance()

                    else -> ctor.callBy(callParams)
                }

                if (csrf != null && !csrfProtection.validateToken(result.toString(), csrf)) {
                    error("Invalid csrf token")
                }

                return@coroutineScope result
            }
        }
    }

    suspend fun convert(value: String, type: KType): Any? {

        // Get the converter class from the shared lookup
        val converterClass = findConverter(type)
            ?: throw NoConverterFoundException("There is no incoming param converter that can handle the type '$type'")

        return converters.get(converterClass).convert(value, type)
    }

    private fun findConverter(type: KType) = lookUp.getOrPut(type) {
        converters.all().firstOrNull { it.canHandle(type) }?.let { it::class }
    }

    private fun decode(str: String) = URLDecoder.decode(str, "UTF-8")
}
