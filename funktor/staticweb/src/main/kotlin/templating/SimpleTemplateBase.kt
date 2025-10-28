package de.peekandpoke.funktor.staticweb.templating

import de.peekandpoke.funktor.staticweb.flashsession.FlashSession
import de.peekandpoke.funktor.staticweb.resources.WebResources
import de.peekandpoke.funktor.staticweb.resources.common.fomanticUi
import de.peekandpoke.funktor.staticweb.resources.common.jQuery
import de.peekandpoke.funktor.staticweb.resources.common.lazySizes
import de.peekandpoke.funktor.staticweb.resources.css
import de.peekandpoke.funktor.staticweb.resources.js
import de.peekandpoke.ultra.semanticui.SemanticTag
import io.ktor.server.html.*
import kotlinx.html.FlowContent
import kotlinx.html.HEAD

abstract class SimpleTemplateBase(
    final override val tools: TemplateTools,
) : SimpleTemplate {
    /** FlashSession service injected from the kontainer */
    final override val flashSession: FlashSession = tools.flash

    /** WebResources service injected from the kontainer */
    final override val webResources: WebResources = tools.resource

    /** Latest entries in the FlashSession */
    val flashSessionEntries = flashSession.pull()

    /** The attributes used on the html tag */
    override val htmlAttributes = mutableMapOf(
        "lang" to "en"
    )

    /** The breadcrumbs of the page */
    override var breadCrumbs: List<Any> = listOf()

    /** Placeholder list for the page head */
    final override val pageHead = PlaceholderList<HEAD, HEAD>()

    /** The main menu of the page */
    final override val mainMenu = Placeholder<FlowContent>()

    /** The background color of the menu */
    override fun SemanticTag.mainMenuBgColor(): SemanticTag = violet

    /** Placeholder for the page content */
    final override val content = PlaceholderList<FlowContent, FlowContent>()

    /** Placeholder list for the pages styles */
    final override val styles = PlaceholderList<HEAD, HEAD>()

    /** Placeholder list for the pages scripts */
    final override val scripts = PlaceholderList<FlowContent, FlowContent>()

    /**
     * Call this to load the default lazy images version
     */
    protected fun loadDefaultLazyImages() {
        styles { css(webResources.lazySizes) }
        scripts { js(webResources.lazySizes) }
    }

    /**
     * Call this to load the default jQuery version
     */
    protected fun loadDefaultJQuery() {
        styles { css(webResources.jQuery) }
        scripts { js(webResources.jQuery) }
    }

    /**
     * Call this to load semantic uis default theme and javascript
     *
     * If you a customized semantic ui, you need to load all resources individually
     */
    protected fun loadDefaultSemanticUi() {
        styles { css(webResources.fomanticUi) }
        scripts { js(webResources.fomanticUi) }
    }
}
