package de.peekandpoke.ultra.semanticui

import kotlinx.html.A
import kotlinx.html.B
import kotlinx.html.BUTTON
import kotlinx.html.DIV
import kotlinx.html.FORM
import kotlinx.html.FlowContent
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
import kotlinx.html.P
import kotlinx.html.SPAN
import kotlinx.html.TABLE
import kotlinx.html.a
import kotlinx.html.b
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.h4
import kotlinx.html.h5
import kotlinx.html.h6
import kotlinx.html.i
import kotlinx.html.img
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.p
import kotlinx.html.span
import kotlinx.html.table

@Suppress("FunctionName", "PropertyName", "unused", "Detekt:TooManyFunctions", "Detekt:VariableNaming")
class SemanticTag(
    private val flow: FlowContent,
    private val cssClasses: MutableList<String>,
) {
    fun _cssClasses(): String = cssClasses.filter { it.isNotBlank() }.joinToString(" ")

    fun _renderAsTag(block: FlowContent.() -> Unit) {
        block(flow)
    }

    // Rendering DIV implicitly as the default

    @SemanticUiCssMarker
    operator fun invoke(flow: DIV.() -> Unit): Unit = Div(flow)

    // Rendering Tags explicitly

    @SemanticUiTagMarker
    infix fun H1(flow: H1.() -> Unit): Unit =
        _renderAsTag { h1(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun H2(flow: H2.() -> Unit): Unit =
        _renderAsTag { h2(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun H3(flow: H3.() -> Unit): Unit =
        _renderAsTag { h3(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun H4(flow: H4.() -> Unit): Unit =
        _renderAsTag { h4(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun H5(flow: H5.() -> Unit): Unit =
        _renderAsTag { h5(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun H6(flow: H6.() -> Unit): Unit =
        _renderAsTag { h6(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun A(flow: A.() -> Unit): Unit =
        _renderAsTag { a(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun B(flow: B.() -> Unit): Unit =
        _renderAsTag { b(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun Button(flow: BUTTON.() -> Unit): Unit =
        _renderAsTag { button(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun Div(flow: DIV.() -> Unit): Unit =
        _renderAsTag { div(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun Form(flow: FORM.() -> Unit): Unit =
        _renderAsTag { form(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun I(flow: I.() -> Unit): Unit =
        _renderAsTag { i(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun Input(flow: INPUT.() -> Unit): Unit =
        _renderAsTag { input(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun Img(flow: IMG.() -> Unit): Unit =
        _renderAsTag { img(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun Label(flow: LABEL.() -> Unit): Unit =
        _renderAsTag { label(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun P(flow: P.() -> Unit): Unit =
        _renderAsTag { p(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun Span(flow: SPAN.() -> Unit): Unit =
        _renderAsTag { span(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun Submit(flow: BUTTON.() -> Unit): Unit =
        _renderAsTag { button(classes = _cssClasses(), block = flow) }

    @SemanticUiTagMarker
    infix fun Table(flow: TABLE.() -> Unit): Unit =
        _renderAsTag { table(classes = _cssClasses(), block = flow) }

    // Adding Custom Css Classes

    @SemanticUiCssMarker
    fun with(block: SemanticTag.() -> SemanticTag): SemanticTag = this.run(block)

    @SemanticUiCssMarker
    fun with(vararg cls: String): SemanticTag = this + cls

    @SemanticUiCssMarker
    fun with(vararg cls: String, flow: DIV.() -> Unit): Unit = (this + cls).invoke(flow)

    @SemanticUiCssMarker
    operator fun plus(cls: String): SemanticTag = apply { cssClasses.add(cls) }

    @SemanticUiCssMarker
    operator fun plus(classes: Array<out String>): SemanticTag = apply { cssClasses.addAll(classes) }

    // Conditional classes

    @SemanticUiConditionalMarker
    fun given(condition: Boolean, action: SemanticTag.() -> SemanticTag): SemanticTag =
        when (condition) {
            false -> this
            else -> this.action()
        }

    @SemanticUiConditionalMarker
    val then: SemanticTag
        get() = this

    // SemanticUI Numbers

    @SemanticUiCssMarker
    fun number(int: Int): SemanticTag = number(SemanticNumber.of(int))

    @SemanticUiCssMarker
    fun number(int: Int, flow: DIV.() -> Unit): Unit = number(SemanticNumber.of(int), flow)

    @SemanticUiCssMarker
    fun number(number: SemanticNumber): SemanticTag = this + number.toString()

    @SemanticUiCssMarker
    fun number(number: SemanticNumber, flow: DIV.() -> Unit): Unit = with(number.toString(), flow = flow)

    @SemanticUiCssMarker
    val one: SemanticTag get() = this + "one"

    @SemanticUiCssMarker
    val two: SemanticTag get() = this + "two"

    @SemanticUiCssMarker
    val three: SemanticTag get() = this + "three"

    @SemanticUiCssMarker
    val four: SemanticTag get() = this + "four"

    @SemanticUiCssMarker
    val five: SemanticTag get() = this + "five"

    @SemanticUiCssMarker
    val six: SemanticTag get() = this + "six"

    @SemanticUiCssMarker
    val seven: SemanticTag get() = this + "seven"

    @SemanticUiCssMarker
    val eight: SemanticTag get() = this + "eight"

    @SemanticUiCssMarker
    val nine: SemanticTag get() = this + "nine"

    @SemanticUiCssMarker
    val ten: SemanticTag get() = this + "ten"

    @SemanticUiCssMarker
    val eleven: SemanticTag get() = this + "eleven"

    @SemanticUiCssMarker
    val twelve: SemanticTag get() = this + "twelve"

    @SemanticUiCssMarker
    val thirteen: SemanticTag get() = this + "thirteen"

    @SemanticUiCssMarker
    val fourteen: SemanticTag get() = this + "fourteen"

    @SemanticUiCssMarker
    val fifteen: SemanticTag get() = this + "fifteen"

    @SemanticUiCssMarker
    val sixteen: SemanticTag get() = this + "sixteen"

    // SemanticUI Colors

    @SemanticUiCssMarker
    fun color(color: SemanticColor): SemanticTag = when {
        color.isSet -> with(color.toString())
        else -> this
    }

    @SemanticUiCssMarker
    fun color(color: SemanticColor, flow: DIV.() -> Unit): Unit = with(color.toString(), flow = flow)

    @SemanticUiCssMarker
    val color: SemanticTag get() = this + "color"

    @SemanticUiCssMarker
    val inverted: SemanticTag get() = this + "inverted"

    @SemanticUiCssMarker
    val red: SemanticTag get() = this + "red"

    @SemanticUiCssMarker
    val orange: SemanticTag get() = this + "orange"

    @SemanticUiCssMarker
    val yellow: SemanticTag get() = this + "yellow"

    @SemanticUiCssMarker
    val olive: SemanticTag get() = this + "olive"

    @SemanticUiCssMarker
    val green: SemanticTag get() = this + "green"

    @SemanticUiCssMarker
    val teal: SemanticTag get() = this + "teal"

    @SemanticUiCssMarker
    val blue: SemanticTag get() = this + "blue"

    @SemanticUiCssMarker
    val violet: SemanticTag get() = this + "violet"

    @SemanticUiCssMarker
    val purple: SemanticTag get() = this + "purple"

    @SemanticUiCssMarker
    val pink: SemanticTag get() = this + "pink"

    @SemanticUiCssMarker
    val brown: SemanticTag get() = this + "brown"

    @SemanticUiCssMarker
    val grey: SemanticTag get() = this + "grey"

    @SemanticUiCssMarker
    val black: SemanticTag get() = this + "black"

    @SemanticUiCssMarker
    val white: SemanticTag get() = this + "white"

    // SemanticUI Sizes

    @SemanticUiCssMarker
    val short: SemanticTag get() = this + "short"

    @SemanticUiCssMarker
    val long: SemanticTag get() = this + "long"

    @SemanticUiCssMarker
    val mini: SemanticTag get() = this + "mini"

    @SemanticUiCssMarker
    val tiny: SemanticTag get() = this + "tiny"

    @SemanticUiCssMarker
    val small: SemanticTag get() = this + "small"

    @SemanticUiCssMarker
    val medium: SemanticTag get() = this + "medium"

    @SemanticUiCssMarker
    val large: SemanticTag get() = this + "large"

    @SemanticUiCssMarker
    val big: SemanticTag get() = this + "big"

    @SemanticUiCssMarker
    val huge: SemanticTag get() = this + "huge"

    @SemanticUiCssMarker
    val massive: SemanticTag get() = this + "massive"

    // SemanticUI Emphasis

    @SemanticUiCssMarker
    val primary: SemanticTag get() = this + "primary"

    @SemanticUiCssMarker
    val secondary: SemanticTag get() = this + "secondary"

    @SemanticUiCssMarker
    val positive: SemanticTag get() = this + "positive"

    @SemanticUiCssMarker
    val negative: SemanticTag get() = this + "negative"

    @SemanticUiCssMarker
    val warning: SemanticTag get() = this + "warning"

    // SemanticUI Brands

    @SemanticUiCssMarker
    val facebook: SemanticTag get() = this + "facebook"

    @SemanticUiCssMarker
    val twitter: SemanticTag get() = this + "twitter"

    @SemanticUiCssMarker
    val google_plus: SemanticTag get() = this + "google plus"

    @SemanticUiCssMarker
    val linkedin: SemanticTag get() = this + "linkedin"

    @SemanticUiCssMarker
    val instagram: SemanticTag get() = this + "instagram"

    @SemanticUiCssMarker
    val youtube: SemanticTag get() = this + "youtube"

    // Semantic UI Words

    @SemanticUiCssMarker
    val icons: SemanticTag get() = this + "icons"

    @SemanticUiCssMarker
    val text: SemanticTag get() = this + "text"

    @SemanticUiCssMarker
    val actions: SemanticTag get() = this + "actions"

    @SemanticUiCssMarker
    val active: SemanticTag get() = this + "active"

    @SemanticUiCssMarker
    val basic: SemanticTag get() = this + "basic"

    @SemanticUiCssMarker
    val bar: SemanticTag get() = this + "bar"

    @SemanticUiCssMarker
    val button: SemanticTag get() = this + "button"

    @SemanticUiCssMarker
    val buttons: SemanticTag get() = this + "buttons"

    @SemanticUiCssMarker
    val circular: SemanticTag get() = this + "circular"

    @SemanticUiCssMarker
    val compact: SemanticTag get() = this + "compact"

    @SemanticUiCssMarker
    val disabled: SemanticTag get() = this + "disabled"

    @SemanticUiCssMarker
    val divider: SemanticTag get() = this + "divider"

    @SemanticUiCssMarker
    val dividing: SemanticTag get() = this + "dividing"

    @SemanticUiCssMarker
    val down: SemanticTag get() = this + "down"

    @SemanticUiCssMarker
    val floated: SemanticTag get() = this + "floated"

    @SemanticUiCssMarker
    val floating: SemanticTag get() = this + "floating"

    @SemanticUiCssMarker
    val fluid: SemanticTag get() = this + "fluid"

    @SemanticUiCssMarker
    val flowing: SemanticTag get() = this + "flowing"

    @SemanticUiCssMarker
    val piled: SemanticTag get() = this + "piled"

    @SemanticUiCssMarker
    val fixed: SemanticTag get() = this + "fixed"

    @SemanticUiCssMarker
    val header: SemanticTag get() = this + "header"

    @SemanticUiCssMarker
    val icon: SemanticTag get() = this + "icon"

    @SemanticUiCssMarker
    val image: SemanticTag get() = this + "image"

    @SemanticUiCssMarker
    val line: SemanticTag get() = this + "line"

    @SemanticUiCssMarker
    val link: SemanticTag get() = this + "link"

    @SemanticUiCssMarker
    val bulleted: SemanticTag get() = this + "bulleted"

    @SemanticUiCssMarker
    val list: SemanticTag get() = this + "list"

    @SemanticUiCssMarker
    val loader: SemanticTag get() = this + "loader"

    @SemanticUiCssMarker
    val loading: SemanticTag get() = this + "loading"

    @SemanticUiCssMarker
    val message: SemanticTag get() = this + "message"

    @SemanticUiCssMarker
    val overlay: SemanticTag get() = this + "overlay"

    @SemanticUiCssMarker
    val pointing: SemanticTag get() = this + "pointing"

    @SemanticUiCssMarker
    val scale: SemanticTag get() = this + "scale"

    @SemanticUiCssMarker
    val shrink: SemanticTag get() = this + "shrink"

    @SemanticUiCssMarker
    val toggle: SemanticTag get() = this + "toggle"

    @SemanticUiCssMarker
    val styled: SemanticTag get() = this + "styled"

    @SemanticUiCssMarker
    val accordion: SemanticTag get() = this + "accordion"

    @SemanticUiCssMarker
    val title: SemanticTag get() = this + "title"

    @SemanticUiCssMarker
    val description: SemanticTag get() = this + "description"

    @SemanticUiCssMarker
    val transition: SemanticTag get() = this + "transition"

    @SemanticUiCssMarker
    val relaxed: SemanticTag get() = this + "relaxed"

    @SemanticUiCssMarker
    val attached: SemanticTag get() = this + "attached"

    @SemanticUiCssMarker
    val clearing: SemanticTag get() = this + "clearing"

    @SemanticUiCssMarker
    val top: SemanticTag get() = this + "top"

    @SemanticUiCssMarker
    val bottom: SemanticTag get() = this + "bottom"

    @SemanticUiCssMarker
    val bordered: SemanticTag get() = this + "bordered"

    @SemanticUiCssMarker
    val avatar: SemanticTag get() = this + "avatar"

    @SemanticUiCssMarker
    val placeholder: SemanticTag get() = this + "placeholder"

    @SemanticUiCssMarker
    val cards: SemanticTag get() = this + "cards"

    @SemanticUiCssMarker
    val card: SemanticTag get() = this + "card"

    @SemanticUiCssMarker
    val raised: SemanticTag get() = this + "raised"

    @SemanticUiCssMarker
    val meta: SemanticTag get() = this + "meta"

    @SemanticUiCssMarker
    val dropdown: SemanticTag get() = this + "dropdown"

    @SemanticUiCssMarker
    val sticky: SemanticTag get() = this + "sticky"

    @SemanticUiCssMarker
    val inline: SemanticTag get() = this + "inline"

    @SemanticUiCssMarker
    val progress: SemanticTag get() = this + "progress"

    @SemanticUiCssMarker
    val paragraph: SemanticTag get() = this + "paragraph"

    @SemanticUiCssMarker
    val slider: SemanticTag get() = this + "slider"

    @SemanticUiCssMarker
    val success: SemanticTag get() = this + "success"

    @SemanticUiCssMarker
    val ordered: SemanticTag get() = this + "ordered"

    @SemanticUiCssMarker
    val steps: SemanticTag get() = this + "steps"

    @SemanticUiCssMarker
    val step: SemanticTag get() = this + "step"

    @SemanticUiCssMarker
    val completed: SemanticTag get() = this + "completed"

    @SemanticUiCssMarker
    val computer: SemanticTag get() = this + "computer"

    @SemanticUiCssMarker
    val tablet: SemanticTag get() = this + "tablet"

    @SemanticUiCssMarker
    val mobile: SemanticTag get() = this + "mobile"

    @SemanticUiCssMarker
    val only: SemanticTag get() = this + "only"

    @SemanticUiCssMarker
    val form: SemanticTag get() = this + "form"

    @SemanticUiCssMarker
    val field: SemanticTag get() = this + "field"

    @SemanticUiCssMarker
    val fields: SemanticTag get() = this + "fields"

    @SemanticUiCssMarker
    val checkbox: SemanticTag get() = this + "checkbox"

    @SemanticUiCssMarker
    val error: SemanticTag get() = this + "error"

    @SemanticUiCssMarker
    val input: SemanticTag get() = this + "input"

    @SemanticUiCssMarker
    val sortable: SemanticTag get() = this + "sortable"

    @SemanticUiCssMarker
    val table: SemanticTag get() = this + "table"

    @SemanticUiCssMarker
    val toast: SemanticTag get() = this + "toast"

    @SemanticUiCssMarker
    val toastBox: SemanticTag get() = this + "toast-box"

    @SemanticUiCssMarker
    val toastContainer: SemanticTag get() = this + "toast-container"

    @SemanticUiCssMarker
    val container: SemanticTag get() = this + "container"

    @SemanticUiCssMarker
    val segment: SemanticTag get() = this + "segment"

    @SemanticUiCssMarker
    val segments: SemanticTag get() = this + "segments"

    @SemanticUiCssMarker
    val pusher: SemanticTag get() = this + "pusher"

    @SemanticUiCssMarker
    val left: SemanticTag get() = this + "left"

    @SemanticUiCssMarker
    val right: SemanticTag get() = this + "right"

    @SemanticUiCssMarker
    val ribbon: SemanticTag get() = this + "ribbon"

    @SemanticUiCssMarker
    val horizontal: SemanticTag get() = this + "horizontal"

    @SemanticUiCssMarker
    val equal: SemanticTag get() = this + "equal"

    @SemanticUiCssMarker
    val width: SemanticTag get() = this + "width"

    @SemanticUiCssMarker
    val vertical: SemanticTag get() = this + "vertical"

    @SemanticUiCssMarker
    val row: SemanticTag get() = this + "row"

    @SemanticUiCssMarker
    val column: SemanticTag get() = this + "column"

    @SemanticUiCssMarker
    val grid: SemanticTag get() = this + "grid"

    @SemanticUiCssMarker
    val wide: SemanticTag get() = this + "wide"

    @SemanticUiCssMarker
    val doubling: SemanticTag get() = this + "doubling"

    @SemanticUiCssMarker
    val stackable: SemanticTag get() = this + "stackable"

    @SemanticUiCssMarker
    val stretched: SemanticTag get() = this + "stretched"

    @SemanticUiCssMarker
    val aligned: SemanticTag get() = this + "aligned"

    @SemanticUiCssMarker
    val fitted: SemanticTag get() = this + "fitted"

    @SemanticUiCssMarker
    val center: SemanticTag get() = this + "center"

    @SemanticUiCssMarker
    val centered: SemanticTag get() = this + "centered"

    @SemanticUiCssMarker
    val divided: SemanticTag get() = this + "divided"

    @SemanticUiCssMarker
    val striped: SemanticTag get() = this + "striped"

    @SemanticUiCssMarker
    val celled: SemanticTag get() = this + "celled"

    @SemanticUiCssMarker
    val selectable: SemanticTag get() = this + "selectable"

    @SemanticUiCssMarker
    val middle: SemanticTag get() = this + "middle"

    @SemanticUiCssMarker
    val padded: SemanticTag get() = this + "padded"

    @SemanticUiCssMarker
    val pagination: SemanticTag get() = this + "pagination"

    @SemanticUiCssMarker
    val animated: SemanticTag get() = this + "animated"

    @SemanticUiCssMarker
    val animating: SemanticTag get() = this + "animating"

    @SemanticUiCssMarker
    val fade: SemanticTag get() = this + "fade"

    @SemanticUiCssMarker
    val out: SemanticTag get() = this + "out"

    @SemanticUiCssMarker
    val _in: SemanticTag get() = this + "in"

    @SemanticUiCssMarker
    val extra: SemanticTag get() = this + "extra"

    @SemanticUiCssMarker
    val very: SemanticTag get() = this + "very"

    @SemanticUiCssMarker
    val content: SemanticTag get() = this + "content"

    @SemanticUiCssMarker
    val scrolling: SemanticTag get() = this + "scrolling"

    @SemanticUiCssMarker
    val visible: SemanticTag get() = this + "visible"

    @SemanticUiCssMarker
    val hidden: SemanticTag get() = this + "hidden"

    @SemanticUiCssMarker
    val label: SemanticTag get() = this + "label"

    @SemanticUiCssMarker
    val tag: SemanticTag get() = this + "tag"

    @SemanticUiCssMarker
    val labeled: SemanticTag get() = this + "labeled"

    @SemanticUiCssMarker
    val menu: SemanticTag get() = this + "menu"

    @SemanticUiCssMarker
    val tab: SemanticTag get() = this + "tab"

    @SemanticUiCssMarker
    val tabular: SemanticTag get() = this + "tabular"

    @SemanticUiCssMarker
    val sidebar: SemanticTag get() = this + "sidebar"

    @SemanticUiCssMarker
    val item: SemanticTag get() = this + "item"

    @SemanticUiCssMarker
    val items: SemanticTag get() = this + "items"

    @SemanticUiCssMarker
    val dimmer: SemanticTag get() = this + "dimmer"

    @SemanticUiCssMarker
    val fullscreen: SemanticTag get() = this + "fullscreen"

    @SemanticUiCssMarker
    val modal: SemanticTag get() = this + "modal"

    @SemanticUiCssMarker
    val modals: SemanticTag get() = this + "modals"

    @SemanticUiCssMarker
    val page: SemanticTag get() = this + "page"

    @SemanticUiCssMarker
    val front: SemanticTag get() = this + "front"

    @SemanticUiCssMarker
    val selection: SemanticTag get() = this + "selection"

    @SemanticUiCssMarker
    val default: SemanticTag get() = this + "default"

    @SemanticUiCssMarker
    val selected: SemanticTag get() = this + "selected"

    @SemanticUiCssMarker
    val search: SemanticTag get() = this + "search"

    @SemanticUiCssMarker
    val popup: SemanticTag get() = this + "popup"
}
