package de.peekandpoke.funktor.staticweb.templating

import de.peekandpoke.funktor.core.broker.TypedRoute
import de.peekandpoke.funktor.staticweb.flashsession.FlashSession
import de.peekandpoke.funktor.staticweb.resources.WebResources
import de.peekandpoke.ultra.semanticui.SemanticTag
import io.ktor.server.html.*
import kotlinx.html.FlowContent
import kotlinx.html.HEAD
import kotlinx.html.HTML
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

interface SimpleTemplate : Template<HTML> {

    /** The Template tools */
    val tools: TemplateTools

    /** Shorthand for accessing the flash session */
    val flashSession: FlashSession

    /** Shorthand for accessing the WebResources */
    val webResources: WebResources

    /** The attributes used on the html tag */
    val htmlAttributes: MutableMap<String, String>

    /**
     * The bread crumbs can be set by each template in order to:
     * - highlight selected main menus
     * - display bread crumbs
     */
    var breadCrumbs: List<Any>

    /** Placeholder for the page title */
    val pageHead: PlaceholderList<HEAD, HEAD>

    /** Placeholder for the styles */
    val styles: PlaceholderList<HEAD, HEAD>

    /** Placeholder for the main menu */
    val mainMenu: Placeholder<FlowContent>

    /** Return the background color of the main menu */
    fun SemanticTag.mainMenuBgColor(): SemanticTag

    /** Placeholder for the content */
    val content: PlaceholderList<FlowContent, FlowContent>

    /** Placeholder for the scripts */
    val scripts: PlaceholderList<FlowContent, FlowContent>

    /**
     * Renders the url of a bound typed route
     */
    val <PARAMS : Any> TypedRoute.Bound<PARAMS>.url get() = tools.routes.render(this)

    override fun HTML.apply() {
        render(this)
    }

    fun render(html: HTML)

    // TODO: create a dedicated formatter and inject it via the template tools
    fun formatDateTime(instant: Instant): String {

        val formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())

        return formatter.format(instant)
    }
}
