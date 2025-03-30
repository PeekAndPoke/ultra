package de.peekandpoke.ktorfx.staticweb.resources

import kotlinx.html.FlowOrMetaDataContent
import kotlinx.html.LinkRel
import kotlinx.html.ScriptType
import kotlinx.html.link
import kotlinx.html.onLoad
import kotlinx.html.script

/**
 * Renders stylesheet tags for each css in the given [group]
 */
fun FlowOrMetaDataContent.css(group: WebResourceGroup) = group.css.forEach { css ->
    link(rel = LinkRel.stylesheet, href = css.fullUri)
}

/**
 * Renders stylesheet tags for each css in the given [group]
 *
 * All sheets in the group will be lazily loaded using this approach:
 * https://stackoverflow.com/questions/32759272/how-to-load-css-asynchronously
 */
fun FlowOrMetaDataContent.lazyCss(group: WebResourceGroup) = group.css.forEach { css ->

    link(rel = LinkRel.stylesheet, href = css.fullUri) {
        media = "none"
        onLoad = "if(media != 'all') media='all'"
    }
}

/**
 * renders script tags for each js in the given [group]
 */
fun FlowOrMetaDataContent.js(group: WebResourceGroup) = group.js.forEach { js ->
    script(type = ScriptType.textJavaScript, src = js.fullUri) {
        //        js.integrity?.let { integrity = it }
    }
}

/**
 * renders script tags for each js in the given [group]
 */
fun FlowOrMetaDataContent.lazyJs(group: WebResourceGroup) = group.js.forEach { js ->
    script(type = ScriptType.textJavaScript, src = js.fullUri) {
        defer = true
        async = true
        //        js.integrity?.let { integrity = it }
    }
}
