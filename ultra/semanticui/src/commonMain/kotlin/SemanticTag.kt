@file:Suppress(
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
    "Detekt:VariableNaming",
)

package io.peekandpoke.ultra.semanticui

import kotlinx.html.A
import kotlinx.html.B
import kotlinx.html.BUTTON
import kotlinx.html.DIV
import kotlinx.html.FORM
import kotlinx.html.H1
import kotlinx.html.H2
import kotlinx.html.H3
import kotlinx.html.H4
import kotlinx.html.H5
import kotlinx.html.H6
import kotlinx.html.I
import kotlinx.html.IMG
import kotlinx.html.INPUT
import kotlinx.html.LABEL
import kotlinx.html.OL
import kotlinx.html.P
import kotlinx.html.SPAN
import kotlinx.html.TABLE
import kotlinx.html.Tag
import kotlinx.html.UL
import kotlinx.html.attributesMapOf

@Suppress("FunctionName", "PropertyName", "unused", "Detekt:TooManyFunctions", "Detekt:VariableNaming")
@SemanticUiDslMarker
class SemanticTag(
    @PublishedApi internal val parent: Tag,
    @PublishedApi internal val cssClasses: MutableList<String>,
) {
    fun _cssClasses(): String = cssClasses.filter { it.isNotBlank() }.joinToString(" ")

    fun _renderAsTag(block: Tag.() -> Unit) {
        block(parent)
    }

    private fun attrs() = attributesMapOf("class", _cssClasses())

    operator fun invoke(block: DIV.() -> Unit = {}): Unit = Div(block)

    infix fun H1(block: H1.() -> Unit): Unit = H1(attrs(), parent.consumer).visitNoInline(block)

    infix fun H2(block: H2.() -> Unit): Unit = H2(attrs(), parent.consumer).visitNoInline(block)

    infix fun H3(block: H3.() -> Unit): Unit = H3(attrs(), parent.consumer).visitNoInline(block)

    infix fun H4(block: H4.() -> Unit): Unit = H4(attrs(), parent.consumer).visitNoInline(block)

    infix fun H5(block: H5.() -> Unit): Unit = H5(attrs(), parent.consumer).visitNoInline(block)

    infix fun H6(block: H6.() -> Unit): Unit = H6(attrs(), parent.consumer).visitNoInline(block)

    infix fun A(block: A.() -> Unit): Unit = A(attrs(), parent.consumer).visitNoInline(block)

    infix fun B(block: B.() -> Unit): Unit = B(attrs(), parent.consumer).visitNoInline(block)

    infix fun Button(block: BUTTON.() -> Unit): Unit = BUTTON(attrs(), parent.consumer).visitNoInline(block)

    infix fun Div(block: DIV.() -> Unit): Unit = DIV(attrs(), parent.consumer).visitNoInline(block)

    infix fun Form(block: FORM.() -> Unit): Unit = FORM(attrs(), parent.consumer).visitNoInline(block)

    infix fun I(block: I.() -> Unit): Unit = I(attrs(), parent.consumer).visitNoInline(block)

    infix fun Input(block: INPUT.() -> Unit): Unit = INPUT(attrs(), parent.consumer).visitNoInline(block)

    infix fun Img(block: IMG.() -> Unit): Unit = IMG(attrs(), parent.consumer).visitNoInline(block)

    infix fun Label(block: LABEL.() -> Unit): Unit = LABEL(attrs(), parent.consumer).visitNoInline(block)

    infix fun Ol(block: OL.() -> Unit): Unit = OL(attrs(), parent.consumer).visitNoInline(block)

    infix fun P(block: P.() -> Unit): Unit = P(attrs(), parent.consumer).visitNoInline(block)

    infix fun Span(block: SPAN.() -> Unit): Unit = SPAN(attrs(), parent.consumer).visitNoInline(block)

    infix fun Submit(block: BUTTON.() -> Unit): Unit = BUTTON(attrs(), parent.consumer).visitNoInline(block)

    infix fun Table(block: TABLE.() -> Unit): Unit = TABLE(attrs(), parent.consumer).visitNoInline(block)

    infix fun Ul(block: UL.() -> Unit): Unit = UL(attrs(), parent.consumer).visitNoInline(block)

    // Adding Css Classes //////////////////////////////////////////////////////////////////////////////////////////////

    operator fun plus(cls: String): SemanticTag = apply { cssClasses.add(cls) }

    operator fun plus(classes: Array<out String>): SemanticTag = apply { cssClasses.addAll(classes) }

    inline fun with(block: SemanticTag.() -> SemanticTag): SemanticTag = this.run(block)

    fun with(vararg cls: String): SemanticTag = this + cls

    fun with(vararg cls: String, flow: DIV.() -> Unit): Unit = (this + cls).invoke(flow)

    // Conditional classes

    fun given(condition: Boolean, fn: SemanticTag.() -> SemanticTag): SemanticTag = when (condition) {
        false -> this
        else -> this.fn()
    }

    fun givenNot(condition: Boolean, fn: SemanticTag.() -> SemanticTag): SemanticTag = given(!condition, fn)

    inline val then: SemanticTag get() = this

    // SemanticUI Numbers //////////////////////////////////////////////////////////////////////////////////////////////

    fun number(int: Int): SemanticTag = number(SemanticNumber.of(int))

    fun number(int: Int, flow: DIV.() -> Unit): Unit = number(SemanticNumber.of(int), flow)

    fun number(number: SemanticNumber): SemanticTag = this + number.toString()

    fun number(number: SemanticNumber, flow: DIV.() -> Unit): Unit = with(number.toString(), flow = flow)

    val one: SemanticTag get() = this + "one"
    val two: SemanticTag get() = this + "two"
    val three: SemanticTag get() = this + "three"
    val four: SemanticTag get() = this + "four"
    val five: SemanticTag get() = this + "five"
    val six: SemanticTag get() = this + "six"
    val seven: SemanticTag get() = this + "seven"
    val eight: SemanticTag get() = this + "eight"
    val nine: SemanticTag get() = this + "nine"
    val ten: SemanticTag get() = this + "ten"
    val eleven: SemanticTag get() = this + "eleven"
    val twelve: SemanticTag get() = this + "twelve"
    val thirteen: SemanticTag get() = this + "thirteen"
    val fourteen: SemanticTag get() = this + "fourteen"
    val fifteen: SemanticTag get() = this + "fifteen"
    val sixteen: SemanticTag get() = this + "sixteen"

    // SemanticUI Colors ///////////////////////////////////////////////////////////////////////////////////////////////


    fun color(color: SemanticColor): SemanticTag = when {
        color.isSet -> with(color.toString())
        else -> this
    }

    fun color(color: SemanticColor, flow: DIV.() -> Unit): Unit = with(color.toString(), flow = flow)

    val color: SemanticTag get() = this + "color"
    val inverted: SemanticTag get() = this + "inverted"
    val red: SemanticTag get() = this + "red"
    val orange: SemanticTag get() = this + "orange"
    val yellow: SemanticTag get() = this + "yellow"
    val olive: SemanticTag get() = this + "olive"
    val green: SemanticTag get() = this + "green"
    val teal: SemanticTag get() = this + "teal"
    val blue: SemanticTag get() = this + "blue"
    val violet: SemanticTag get() = this + "violet"
    val purple: SemanticTag get() = this + "purple"
    val pink: SemanticTag get() = this + "pink"
    val brown: SemanticTag get() = this + "brown"
    val grey: SemanticTag get() = this + "grey"
    val black: SemanticTag get() = this + "black"
    val white: SemanticTag get() = this + "white"

    // Sizes ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    val short: SemanticTag get() = this + "short"
    val long: SemanticTag get() = this + "long"

    val full: SemanticTag get() = this + "full"

    val tall: SemanticTag get() = this + "tall"

    val mini: SemanticTag get() = this + "mini"
    val tiny: SemanticTag get() = this + "tiny"
    val small: SemanticTag get() = this + "small"
    val medium: SemanticTag get() = this + "medium"
    val large: SemanticTag get() = this + "large"
    val big: SemanticTag get() = this + "big"
    val huge: SemanticTag get() = this + "huge"
    val massive: SemanticTag get() = this + "massive"

    // SemanticUI Emphasis /////////////////////////////////////////////////////////////////////////////////////////////

    val primary: SemanticTag get() = this + "primary"
    val secondary: SemanticTag get() = this + "secondary"
    val tertiary: SemanticTag get() = this + "tertiary"

    val positive: SemanticTag get() = this + "positive"
    val negative: SemanticTag get() = this + "negative"

    val info: SemanticTag get() = this + "info"
    val warning: SemanticTag get() = this + "warning"
    val error: SemanticTag get() = this + "error"

    // SemanticUI Brands

    val facebook: SemanticTag get() = this + "facebook"
    val google_plus: SemanticTag get() = this + "google plus"
    val instagram: SemanticTag get() = this + "instagram"
    val linkedin: SemanticTag get() = this + "linkedin"
    val telegram: SemanticTag get() = this + "telegram"
    val twitter: SemanticTag get() = this + "twitter"
    val vk: SemanticTag get() = this + "vk"
    val whatsapp: SemanticTag get() = this + "whatsapp"
    val youtube: SemanticTag get() = this + "youtube"

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Semantic UI Words ///////////////////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    val _in: SemanticTag get() = this + "in"
    val accordion: SemanticTag get() = this + "accordion"
    val action: SemanticTag get() = this + "action"
    val actions: SemanticTag get() = this + "actions"
    val active: SemanticTag get() = this + "active"
    val ad: SemanticTag get() = this + "ad"
    val aligned: SemanticTag get() = this + "aligned"
    val animated: SemanticTag get() = this + "animated"
    val animating: SemanticTag get() = this + "animating"
    val attached: SemanticTag get() = this + "attached"
    val author: SemanticTag get() = this + "author"
    val avatar: SemanticTag get() = this + "avatar"
    val bar: SemanticTag get() = this + "bar"
    val basic: SemanticTag get() = this + "basic"
    val below: SemanticTag get() = this + "below"
    val billboard: SemanticTag get() = this + "billboard"
    val block: SemanticTag get() = this + "block"
    val banner: SemanticTag get() = this + "banner"
    val borderless: SemanticTag get() = this + "borderless"
    val bordered: SemanticTag get() = this + "bordered"
    val bottom: SemanticTag get() = this + "bottom"
    val breadcrumb: SemanticTag get() = this + "breadcrumb"
    val bulleted: SemanticTag get() = this + "bulleted"
    val button: SemanticTag get() = this + "button"
    val buttons: SemanticTag get() = this + "buttons"
    val card: SemanticTag get() = this + "card"
    val cards: SemanticTag get() = this + "cards"
    val celled: SemanticTag get() = this + "celled"
    val center: SemanticTag get() = this + "center"
    val centered: SemanticTag get() = this + "centered"
    val checkbox: SemanticTag get() = this + "checkbox"
    val circular: SemanticTag get() = this + "circular"
    val clearing: SemanticTag get() = this + "clearing"
    val close: SemanticTag get() = this + "close"
    val collapsing: SemanticTag get() = this + "collapsing"
    val column: SemanticTag get() = this + "column"
    val comment: SemanticTag get() = this + "comment"
    val connected: SemanticTag get() = this + "connected"
    val comments: SemanticTag get() = this + "comments"
    val compact: SemanticTag get() = this + "compact"
    val completed: SemanticTag get() = this + "completed"
    val computer: SemanticTag get() = this + "computer"
    val container: SemanticTag get() = this + "container"
    val content: SemanticTag get() = this + "content"
    val corner: SemanticTag get() = this + "corner"
    val date: SemanticTag get() = this + "date"
    val default: SemanticTag get() = this + "default"
    val definition: SemanticTag get() = this + "definition"
    val description: SemanticTag get() = this + "description"
    val detail: SemanticTag get() = this + "detail"
    val dimmer: SemanticTag get() = this + "dimmer"
    val disabled: SemanticTag get() = this + "disabled"
    val divided: SemanticTag get() = this + "divided"
    val divider: SemanticTag get() = this + "divider"
    val dividing: SemanticTag get() = this + "dividing"
    val double: SemanticTag get() = this + "double"
    val doubling: SemanticTag get() = this + "doubling"
    val down: SemanticTag get() = this + "down"
    val dropdown: SemanticTag get() = this + "dropdown"
    val elastic: SemanticTag get() = this + "elastic"
    val empty: SemanticTag get() = this + "empty"
    val equal: SemanticTag get() = this + "equal"
    val event: SemanticTag get() = this + "event"
    val extra: SemanticTag get() = this + "extra"
    val fade: SemanticTag get() = this + "fade"
    val fast: SemanticTag get() = this + "fast"
    val feed: SemanticTag get() = this + "feed"
    val field: SemanticTag get() = this + "field"
    val fields: SemanticTag get() = this + "fields"
    val fitted: SemanticTag get() = this + "fitted"
    val fixed: SemanticTag get() = this + "fixed"
    val floated: SemanticTag get() = this + "floated"
    val focus: SemanticTag get() = this + "focus"
    val floating: SemanticTag get() = this + "floating"
    val flowing: SemanticTag get() = this + "flowing"
    val fluid: SemanticTag get() = this + "fluid"
    val form: SemanticTag get() = this + "form"
    val front: SemanticTag get() = this + "front"
    val fullscreen: SemanticTag get() = this + "fullscreen"
    val grid: SemanticTag get() = this + "grid"
    val grouped: SemanticTag get() = this + "grouped"
    val half: SemanticTag get() = this + "half"
    val header: SemanticTag get() = this + "header"
    val hidden: SemanticTag get() = this + "hidden"
    val horizontal: SemanticTag get() = this + "horizontal"
    val horizontally: SemanticTag get() = this + "horizontally"
    val icon: SemanticTag get() = this + "icon"
    val icons: SemanticTag get() = this + "icons"
    val image: SemanticTag get() = this + "image"
    val images: SemanticTag get() = this + "images"
    val indeterminate: SemanticTag get() = this + "indeterminate"
    val inline: SemanticTag get() = this + "inline"
    val input: SemanticTag get() = this + "input"
    val instant: SemanticTag get() = this + "instant"
    val internal: SemanticTag get() = this + "internal"
    val internally: SemanticTag get() = this + "internally"
    val item: SemanticTag get() = this + "item"
    val items: SemanticTag get() = this + "items"
    val justified: SemanticTag get() = this + "justified"
    val label: SemanticTag get() = this + "label"
    val labeled: SemanticTag get() = this + "labeled"
    val labels: SemanticTag get() = this + "labels"
    val leaderboard: SemanticTag get() = this + "leaderboard"
    val left: SemanticTag get() = this + "left"
    val like: SemanticTag get() = this + "like"
    val line: SemanticTag get() = this + "line"
    val link: SemanticTag get() = this + "link"
    val list: SemanticTag get() = this + "list"
    val loader: SemanticTag get() = this + "loader"
    val loading: SemanticTag get() = this + "loading"
    val logo: SemanticTag get() = this + "logo"
    val menu: SemanticTag get() = this + "menu"
    val message: SemanticTag get() = this + "message"
    val meta: SemanticTag get() = this + "meta"
    val metadata: SemanticTag get() = this + "metadata"
    val middle: SemanticTag get() = this + "middle"
    val minimal: SemanticTag get() = this + "minimal"
    val mobile: SemanticTag get() = this + "mobile"
    val modal: SemanticTag get() = this + "modal"
    val modals: SemanticTag get() = this + "modals"
    val move: SemanticTag get() = this + "move"
    val only: SemanticTag get() = this + "only"
    val ordered: SemanticTag get() = this + "ordered"
    val out: SemanticTag get() = this + "out"
    val overlay: SemanticTag get() = this + "overlay"
    val padded: SemanticTag get() = this + "padded"
    val page: SemanticTag get() = this + "page"
    val pagination: SemanticTag get() = this + "pagination"
    val paragraph: SemanticTag get() = this + "paragraph"
    val piled: SemanticTag get() = this + "piled"
    val placeholder: SemanticTag get() = this + "placeholder"
    val pointing: SemanticTag get() = this + "pointing"
    val popup: SemanticTag get() = this + "popup"
    val progress: SemanticTag get() = this + "progress"
    val pusher: SemanticTag get() = this + "pusher"
    val rail: SemanticTag get() = this + "rail"
    val raised: SemanticTag get() = this + "raised"
    val rectangle: SemanticTag get() = this + "rectangle"
    val rectangular: SemanticTag get() = this + "rectangular"
    val relaxed: SemanticTag get() = this + "relaxed"
    val reply: SemanticTag get() = this + "reply"
    val required: SemanticTag get() = this + "required"
    val reveal: SemanticTag get() = this + "reveal"
    val reversed: SemanticTag get() = this + "reversed"
    val ribbon: SemanticTag get() = this + "ribbon"
    val right: SemanticTag get() = this + "right"
    val rotate: SemanticTag get() = this + "rotate"
    val rounded: SemanticTag get() = this + "rounded"
    val row: SemanticTag get() = this + "row"
    val scale: SemanticTag get() = this + "scale"
    val scrolling: SemanticTag get() = this + "scrolling"
    val search: SemanticTag get() = this + "search"
    val section: SemanticTag get() = this + "section"
    val segment: SemanticTag get() = this + "segment"
    val segments: SemanticTag get() = this + "segments"
    val selectable: SemanticTag get() = this + "selectable"
    val selected: SemanticTag get() = this + "selected"
    val selection: SemanticTag get() = this + "selection"
    val shrink: SemanticTag get() = this + "shrink"
    val sidebar: SemanticTag get() = this + "sidebar"
    val single: SemanticTag get() = this + "single"
    val skyscraper: SemanticTag get() = this + "skyscraper"
    val slider: SemanticTag get() = this + "slider"
    val slow: SemanticTag get() = this + "slow"
    val sortable: SemanticTag get() = this + "sortable"
    val spaced: SemanticTag get() = this + "spaced"
    val square: SemanticTag get() = this + "square"
    val stackable: SemanticTag get() = this + "stackable"
    val stacked: SemanticTag get() = this + "stacked"
    val statistic: SemanticTag get() = this + "statistic"
    val statistics: SemanticTag get() = this + "statistics"
    val step: SemanticTag get() = this + "step"
    val steps: SemanticTag get() = this + "steps"
    val sticky: SemanticTag get() = this + "sticky"
    val stretched: SemanticTag get() = this + "stretched"
    val striped: SemanticTag get() = this + "striped"
    val styled: SemanticTag get() = this + "styled"
    val sub: SemanticTag get() = this + "sub"
    val success: SemanticTag get() = this + "success"
    val summary: SemanticTag get() = this + "summary"
    val tab: SemanticTag get() = this + "tab"
    val table: SemanticTag get() = this + "table"
    val tablet: SemanticTag get() = this + "tablet"
    val tabular: SemanticTag get() = this + "tabular"
    val tag: SemanticTag get() = this + "tag"
    val test: SemanticTag get() = this + "test"
    val text: SemanticTag get() = this + "text"
    val threaded: SemanticTag get() = this + "threaded"
    val title: SemanticTag get() = this + "title"
    val toast: SemanticTag get() = this + "toast"
    val toastBox: SemanticTag get() = this + "toast-box"
    val toastContainer: SemanticTag get() = this + "toast-container"
    val toggle: SemanticTag get() = this + "toggle"
    val top: SemanticTag get() = this + "top"
    val transition: SemanticTag get() = this + "transition"
    val transparent: SemanticTag get() = this + "transparent"
    val unstackable: SemanticTag get() = this + "unstackable"
    val up: SemanticTag get() = this + "up"
    val usual: SemanticTag get() = this + "usual"
    val value: SemanticTag get() = this + "value"
    val vertical: SemanticTag get() = this + "vertical"
    val vertically: SemanticTag get() = this + "vertically"
    val very: SemanticTag get() = this + "very"
    val visible: SemanticTag get() = this + "visible"
    val wide: SemanticTag get() = this + "wide"
    val width: SemanticTag get() = this + "width"
    val wireframe: SemanticTag get() = this + "wireframe"
}
