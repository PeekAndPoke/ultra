package de.peekandpoke.funktor.staticweb.resources

import de.peekandpoke.funktor.core.model.CacheBuster
import de.peekandpoke.ultra.common.md5
import de.peekandpoke.ultra.common.sha384
import de.peekandpoke.ultra.common.toBase64
import java.io.InputStream

/**
 * Abstract base for all resource groups
 *
 * Derive from this class and register all resources in the [builder]
 *
 * The [cacheBuster] is passed down to each [WebResource]
 */
abstract class WebResourceGroup(

    private val cacheBuster: CacheBuster,
    private val builder: Builder.() -> Unit,

    ) {

    /**
     * A list of all css files in the group
     */
    val css: List<WebResource>

    /**
     * A list of all js files in the group
     */
    val js: List<WebResource>

    init {
        val loader = Thread.currentThread().contextClassLoader ?: this::class.java.classLoader

        Builder(loader, cacheBuster).apply(builder).apply {
            css = getCss()
            js = getJs()
        }
    }

    class Builder(private val loader: ClassLoader, private val cacheBuster: CacheBuster) {

        private val css = mutableListOf<WebResource>()
        private val js = mutableListOf<WebResource>()

        internal fun getCss() = css.toList()

        internal fun getJs() = js.toList()

        fun webjarCss(uri: String): WebResource {
            return WebResource(uri, cacheBuster.key).apply {
                css.add(this)
            }
        }

        fun resourceCss(uri: String): WebResource {

            val stream = uri.toInputStream()
            val bytes = stream.readBytes()

            stream.close()

            return WebResource(uri, bytes.md5(), "sha384-${bytes.sha384().toBase64()}").apply {
                css.add(this)
            }
        }

        fun webjarJs(uri: String): WebResource {
            return WebResource(uri, cacheBuster.key).apply {
                js.add(this)
            }
        }

        fun resourceJs(uri: String): WebResource {

            val bytes = uri.toInputStream().readBytes()

            return WebResource(uri, bytes.md5(), "sha384-${bytes.sha384().toBase64()}").apply {
                js.add(this)
            }
        }

        private fun String.toInputStream(): InputStream {

            val stream = loader.getResourceAsStream(this.trimStart('/'))

            // TODO: custom exception
            return stream ?: error("Resource '$this' is not present")
        }
    }
}
