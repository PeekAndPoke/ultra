package io.peekandpoke.kraft.addons.images

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.ImageSizes
import io.peekandpoke.ultra.html.ImageSrcSet
import kotlinx.html.IMG
import kotlinx.html.Tag
import kotlinx.html.img

@Suppress("FunctionName")
fun Tag.SrcSetImage(
    src: String,
    sizes: ImageSizes = ImageSizes.default,
    alt: String = "",
    configure: IMG.() -> Unit = {},
) = comp(
    SrcSetImage.Props(
        src = src,
        sizes = sizes,
        alt = alt,
        configure = configure
    )
) {
    SrcSetImage(it)
}

class SrcSetImage(ctx: Ctx<Props>) : Component<SrcSetImage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        var src: String,
        var sizes: ImageSizes,
        var alt: String,
        var configure: IMG.() -> Unit = {},
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private fun getImageSrcSet(): ImageSrcSet = ImageSrcSet.auto(src = props.src, sizes = props.sizes)

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        val imageSrcSet = getImageSrcSet()

        img(src = props.src, alt = props.alt) {

            props.configure(this)

            if (imageSrcSet.entries.isNotEmpty()) {

                imageSrcSet.entries.let { entries ->
                    // sizes attribute for image cdn
                    attributes["sizes"] = props.sizes.render()

                    // src set
                    if (imageSrcSet.entries.isNotEmpty()) {
                        attributes["srcset"] = entries.joinToString(",") { "${it.url} ${it.maxWidth}w" }
                    }
                }
            }
        }
    }
}
