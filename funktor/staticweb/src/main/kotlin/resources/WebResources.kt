package de.peekandpoke.ktorfx.staticweb.resources

import de.peekandpoke.ktorfx.core.model.CacheBuster
import de.peekandpoke.ultra.common.Lookup
import de.peekandpoke.ultra.common.md5
import de.peekandpoke.ultra.common.sha384
import de.peekandpoke.ultra.common.toBase64
import org.webjars.WebJarAssetLocator
import java.io.InputStream
import kotlin.reflect.KClass

/**
 * Main class that collects all [WebResourceGroup]
 */
class WebResources(
//    private val log: Log,
    private val groups: Lookup<WebResourceGroup>,
    private val cacheBuster: CacheBuster,
) {
    companion object {
        private val combinedCache = mutableMapOf<List<WebResource>, CombinedResources>()
    }

    data class CombinedResources(
        val resources: List<WebResource>,
        val combined: String,
        val cacheKey: String,
        val integrity: String?,
    )

    private val webjarsLocator by lazy { WebJarAssetLocator() }
    private val knownWebJars by lazy { webjarsLocator.webJars.keys.toSet() }
    private val loader by lazy { Thread.currentThread().contextClassLoader ?: this::class.java.classLoader }

//    init {
//        groups.all().forEach {
//            log.trace("Resource group $it OK")
//        }
//    }

    /**
     * Get a resource group by its [cls]
     *
     * If the group is not present a [WebResourceGroupNotFound] exception is thrown
     */
    operator fun <T : WebResourceGroup> get(cls: KClass<T>): T = groups.getOrNull(cls)
        ?: throw WebResourceGroupNotFound("Resource group '${cls.qualifiedName}' is not present.")


    fun combine(resources: List<WebResource>, webjarsPrefix: String): CombinedResources {

        return combinedCache.getOrPut(resources.toList()) {
            val combined = resources.joinToString("\n\n") { resource ->
                val stream = locateResource(resource.uri)
                    ?: locatedWebjar(resource.uri, webjarsPrefix)
                    ?: throw WebResourceNotFound(resource.uri)

                listOf(
                    "/* ************************************************************************** */",
                    "/* ${resource.uri} */",
                    "/* ************************************************************************** */",
                    stream.readBytes().toString(Charsets.UTF_8),
                ).joinToString("\n")
            }

            val sha384 = combined.sha384().toBase64()
            val integrity = "sha384-$sha384"

            return CombinedResources(
                resources = resources,
                combined = combined,
                cacheKey = combined.md5() + '-' + cacheBuster.key,
                integrity = integrity,
            )
        }
    }

    private fun locateResource(uri: String): InputStream? {
        return loader.getResourceAsStream(uri.trimStart('/'))
    }

    private fun locatedWebjar(uri: String, webjarsPrefix: String): InputStream? {
        val resourcePath = uri.removePrefix('/' + webjarsPrefix.trimStart('/'))
        val location = extractWebJar(resourcePath, knownWebJars, webjarsLocator)

        return loader.getResourceAsStream(location)
    }

    private fun extractWebJar(path: String, knownWebJars: Set<String>, locator: WebJarAssetLocator): String {
        val firstDelimiter = if (path.startsWith("/")) 1 else 0
        val nextDelimiter = path.indexOf("/", 1)
        val webjar = if (nextDelimiter > -1) path.substring(firstDelimiter, nextDelimiter) else ""
        val partialPath = path.substring(nextDelimiter + 1)
        if (webjar !in knownWebJars) {
            throw IllegalArgumentException("jar $webjar not found")
        }
        return locator.getFullPath(webjar, partialPath)
    }
}
