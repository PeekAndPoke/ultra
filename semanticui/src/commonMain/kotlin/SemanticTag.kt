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
import kotlinx.html.Tag
import kotlinx.html.attributesMapOf
import kotlin.js.JsName

@Suppress("FunctionName", "PropertyName", "unused", "Detekt:TooManyFunctions", "Detekt:VariableNaming")
class SemanticTag(
    @PublishedApi internal val flow: FlowContent,
    @PublishedApi internal val cssClasses: MutableList<String>,
) {
    fun _cssClasses(): String = cssClasses.filter { it.isNotBlank() }.joinToString(" ")

    fun _renderAsTag(block: FlowContent.() -> Unit) {
        block(flow)
    }

    private fun attrs() = attributesMapOf("class", _cssClasses())

    private inline fun <T : Tag> T.visit(block: T.() -> Unit) {
        consumer.onTagStart(this)
        try {
            this.block()
        } catch (err: Throwable) {
            consumer.onTagError(this, err)
        } finally {
            consumer.onTagEnd(this)
        }
    }

    @SemanticUiCssMarker
    @JsName("i")
    operator fun invoke(block: DIV.() -> Unit): Unit = Div(block)

    @SemanticUiTagMarker
    @JsName("H1")
    infix fun H1(block: H1.() -> Unit): Unit = H1(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("H2")
    infix fun H2(block: H2.() -> Unit): Unit = H2(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("H3")
    infix fun H3(block: H3.() -> Unit): Unit = H3(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("H4")
    infix fun H4(block: H4.() -> Unit): Unit = H4(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("H5")
    infix fun H5(block: H5.() -> Unit): Unit = H5(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("H6")
    infix fun H6(block: H6.() -> Unit): Unit = H6(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("A")
    infix fun A(block: A.() -> Unit): Unit = A(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("B")
    infix fun B(block: B.() -> Unit): Unit = B(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("Button")
    infix fun Button(block: BUTTON.() -> Unit): Unit = BUTTON(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("Div")
    infix fun Div(block: DIV.() -> Unit): Unit = DIV(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("Form")
    infix fun Form(block: FORM.() -> Unit): Unit = FORM(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("I")
    infix fun I(block: I.() -> Unit): Unit = I(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("Input")
    infix fun Input(block: INPUT.() -> Unit): Unit = INPUT(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("Img")
    infix fun Img(block: IMG.() -> Unit): Unit = IMG(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("Label")
    infix fun Label(block: LABEL.() -> Unit): Unit = LABEL(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("P")
    infix fun P(block: P.() -> Unit): Unit = P(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("Span")
    infix fun Span(block: SPAN.() -> Unit): Unit = SPAN(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("Submit")
    infix fun Submit(block: BUTTON.() -> Unit): Unit = BUTTON(attrs(), flow.consumer).visit(block)

    @SemanticUiTagMarker
    @JsName("Table")
    infix fun Table(block: TABLE.() -> Unit): Unit = TABLE(attrs(), flow.consumer).visit(block)

    // Adding Css Classes //////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker
    @JsName("p")
    operator fun plus(cls: String): SemanticTag = apply { cssClasses.add(cls) }

    @SemanticUiCssMarker
    @JsName("p2")
    operator fun plus(classes: Array<out String>): SemanticTag = apply { cssClasses.addAll(classes) }

    @SemanticUiCssMarker
    inline fun with(block: SemanticTag.() -> SemanticTag): SemanticTag = this.run(block)

    @SemanticUiCssMarker
    @JsName("w")
    fun with(vararg cls: String): SemanticTag = this + cls

    @SemanticUiCssMarker
    @JsName("w2")
    fun with(vararg cls: String, flow: DIV.() -> Unit): Unit = (this + cls).invoke(flow)

    // Conditional classes

    @SemanticUiConditionalMarker
    @JsName("g")
    fun given(condition: Boolean, action: SemanticTag.() -> SemanticTag): SemanticTag =
        when (condition) {
            false -> this
            else -> this.action()
        }

    @SemanticUiConditionalMarker
    @JsName("gn")
    fun givenNot(condition: Boolean, action: SemanticTag.() -> SemanticTag): SemanticTag =
        given(!condition, action)

    @SemanticUiConditionalMarker
    inline val then: SemanticTag get() = this

    // SemanticUI Numbers //////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker
    @JsName("n")
    fun number(int: Int): SemanticTag = number(SemanticNumber.of(int))

    @SemanticUiCssMarker
    @JsName("n2")
    fun number(int: Int, flow: DIV.() -> Unit): Unit = number(SemanticNumber.of(int), flow)

    @SemanticUiCssMarker
    @JsName("n3")
    fun number(number: SemanticNumber): SemanticTag = this + number.toString()

    @SemanticUiCssMarker
    @JsName("n4")
    fun number(number: SemanticNumber, flow: DIV.() -> Unit): Unit = with(number.toString(), flow = flow)

    @SemanticUiCssMarker
    inline val one: SemanticTag get() = this + "one"

    @SemanticUiCssMarker
    inline val two: SemanticTag get() = this + "two"

    @SemanticUiCssMarker
    inline val three: SemanticTag get() = this + "three"

    @SemanticUiCssMarker
    inline val four: SemanticTag get() = this + "four"

    @SemanticUiCssMarker
    inline val five: SemanticTag get() = this + "five"

    @SemanticUiCssMarker
    inline val six: SemanticTag get() = this + "six"

    @SemanticUiCssMarker
    inline val seven: SemanticTag get() = this + "seven"

    @SemanticUiCssMarker
    inline val eight: SemanticTag get() = this + "eight"

    @SemanticUiCssMarker
    inline val nine: SemanticTag get() = this + "nine"

    @SemanticUiCssMarker
    inline val ten: SemanticTag get() = this + "ten"

    @SemanticUiCssMarker
    inline val eleven: SemanticTag get() = this + "eleven"

    @SemanticUiCssMarker
    inline val twelve: SemanticTag get() = this + "twelve"

    @SemanticUiCssMarker
    inline val thirteen: SemanticTag get() = this + "thirteen"

    @SemanticUiCssMarker
    inline val fourteen: SemanticTag get() = this + "fourteen"

    @SemanticUiCssMarker
    inline val fifteen: SemanticTag get() = this + "fifteen"

    @SemanticUiCssMarker
    inline val sixteen: SemanticTag get() = this + "sixteen"

    // SemanticUI Colors ///////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker
    @JsName("c")
    fun color(color: SemanticColor): SemanticTag = when {
        color.isSet -> with(color.toString())
        else -> this
    }

    @SemanticUiCssMarker
    @JsName("c2")
    fun color(color: SemanticColor, flow: DIV.() -> Unit): Unit = with(color.toString(), flow = flow)

    @SemanticUiCssMarker
    inline val color: SemanticTag get() = this + "color"

    @SemanticUiCssMarker
    inline val inverted: SemanticTag get() = this + "inverted"

    @SemanticUiCssMarker
    inline val red: SemanticTag get() = this + "red"

    @SemanticUiCssMarker
    inline val orange: SemanticTag get() = this + "orange"

    @SemanticUiCssMarker
    inline val yellow: SemanticTag get() = this + "yellow"

    @SemanticUiCssMarker
    inline val olive: SemanticTag get() = this + "olive"

    @SemanticUiCssMarker
    inline val green: SemanticTag get() = this + "green"

    @SemanticUiCssMarker
    inline val teal: SemanticTag get() = this + "teal"

    @SemanticUiCssMarker
    inline val blue: SemanticTag get() = this + "blue"

    @SemanticUiCssMarker
    inline val violet: SemanticTag get() = this + "violet"

    @SemanticUiCssMarker
    inline val purple: SemanticTag get() = this + "purple"

    @SemanticUiCssMarker
    inline val pink: SemanticTag get() = this + "pink"

    @SemanticUiCssMarker
    inline val brown: SemanticTag get() = this + "brown"

    @SemanticUiCssMarker
    inline val grey: SemanticTag get() = this + "grey"

    @SemanticUiCssMarker
    inline val black: SemanticTag get() = this + "black"

    @SemanticUiCssMarker
    inline val white: SemanticTag get() = this + "white"

    // Sizes ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker
    inline val short: SemanticTag get() = this + "short"

    @SemanticUiCssMarker
    inline val long: SemanticTag get() = this + "long"

    @SemanticUiCssMarker
    inline val mini: SemanticTag get() = this + "mini"

    @SemanticUiCssMarker
    inline val tiny: SemanticTag get() = this + "tiny"

    @SemanticUiCssMarker
    inline val small: SemanticTag get() = this + "small"

    @SemanticUiCssMarker
    inline val medium: SemanticTag get() = this + "medium"

    @SemanticUiCssMarker
    inline val large: SemanticTag get() = this + "large"

    @SemanticUiCssMarker
    inline val big: SemanticTag get() = this + "big"

    @SemanticUiCssMarker
    inline val huge: SemanticTag get() = this + "huge"

    @SemanticUiCssMarker
    inline val massive: SemanticTag get() = this + "massive"

    // SemanticUI Emphasis /////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker
    inline val primary: SemanticTag get() = this + "primary"

    @SemanticUiCssMarker
    inline val secondary: SemanticTag get() = this + "secondary"

    @SemanticUiCssMarker
    inline val positive: SemanticTag get() = this + "positive"

    @SemanticUiCssMarker
    inline val negative: SemanticTag get() = this + "negative"

    @SemanticUiCssMarker
    inline val warning: SemanticTag get() = this + "warning"

    // SemanticUI Brands

    @SemanticUiCssMarker
    inline val facebook: SemanticTag get() = this + "facebook"

    @SemanticUiCssMarker
    inline val twitter: SemanticTag get() = this + "twitter"

    @SemanticUiCssMarker
    inline val google_plus: SemanticTag get() = this + "google plus"

    @SemanticUiCssMarker
    inline val linkedin: SemanticTag get() = this + "linkedin"

    @SemanticUiCssMarker
    inline val instagram: SemanticTag get() = this + "instagram"

    @SemanticUiCssMarker
    inline val youtube: SemanticTag get() = this + "youtube"

    // Semantic UI Words

    @SemanticUiCssMarker
    inline val icons: SemanticTag get() = this + "icons"

    @SemanticUiCssMarker
    inline val text: SemanticTag get() = this + "text"

    @SemanticUiCssMarker
    inline val actions: SemanticTag get() = this + "actions"

    @SemanticUiCssMarker
    inline val active: SemanticTag get() = this + "active"

    @SemanticUiCssMarker
    inline val basic: SemanticTag get() = this + "basic"

    @SemanticUiCssMarker
    inline val bar: SemanticTag get() = this + "bar"

    @SemanticUiCssMarker
    inline val button: SemanticTag get() = this + "button"

    @SemanticUiCssMarker
    inline val buttons: SemanticTag get() = this + "buttons"

    @SemanticUiCssMarker
    inline val circular: SemanticTag get() = this + "circular"

    @SemanticUiCssMarker
    inline val compact: SemanticTag get() = this + "compact"

    @SemanticUiCssMarker
    inline val disabled: SemanticTag get() = this + "disabled"

    @SemanticUiCssMarker
    inline val divider: SemanticTag get() = this + "divider"

    @SemanticUiCssMarker
    inline val dividing: SemanticTag get() = this + "dividing"

    @SemanticUiCssMarker
    inline val down: SemanticTag get() = this + "down"

    @SemanticUiCssMarker
    inline val floated: SemanticTag get() = this + "floated"

    @SemanticUiCssMarker
    inline val floating: SemanticTag get() = this + "floating"

    @SemanticUiCssMarker
    inline val fluid: SemanticTag get() = this + "fluid"

    @SemanticUiCssMarker
    inline val flowing: SemanticTag get() = this + "flowing"

    @SemanticUiCssMarker
    inline val piled: SemanticTag get() = this + "piled"

    @SemanticUiCssMarker
    inline val fixed: SemanticTag get() = this + "fixed"

    @SemanticUiCssMarker
    inline val header: SemanticTag get() = this + "header"

    @SemanticUiCssMarker
    inline val icon: SemanticTag get() = this + "icon"

    @SemanticUiCssMarker
    inline val image: SemanticTag get() = this + "image"

    @SemanticUiCssMarker
    inline val line: SemanticTag get() = this + "line"

    @SemanticUiCssMarker
    inline val link: SemanticTag get() = this + "link"

    @SemanticUiCssMarker
    inline val bulleted: SemanticTag get() = this + "bulleted"

    @SemanticUiCssMarker
    inline val list: SemanticTag get() = this + "list"

    @SemanticUiCssMarker
    inline val loader: SemanticTag get() = this + "loader"

    @SemanticUiCssMarker
    inline val loading: SemanticTag get() = this + "loading"

    @SemanticUiCssMarker
    inline val message: SemanticTag get() = this + "message"

    @SemanticUiCssMarker
    inline val overlay: SemanticTag get() = this + "overlay"

    @SemanticUiCssMarker
    inline val pointing: SemanticTag get() = this + "pointing"

    @SemanticUiCssMarker
    inline val scale: SemanticTag get() = this + "scale"

    @SemanticUiCssMarker
    inline val shrink: SemanticTag get() = this + "shrink"

    @SemanticUiCssMarker
    inline val toggle: SemanticTag get() = this + "toggle"

    @SemanticUiCssMarker
    inline val styled: SemanticTag get() = this + "styled"

    @SemanticUiCssMarker
    inline val accordion: SemanticTag get() = this + "accordion"

    @SemanticUiCssMarker
    inline val title: SemanticTag get() = this + "title"

    @SemanticUiCssMarker
    inline val description: SemanticTag get() = this + "description"

    @SemanticUiCssMarker
    inline val transition: SemanticTag get() = this + "transition"

    @SemanticUiCssMarker
    inline val relaxed: SemanticTag get() = this + "relaxed"

    @SemanticUiCssMarker
    inline val attached: SemanticTag get() = this + "attached"

    @SemanticUiCssMarker
    inline val clearing: SemanticTag get() = this + "clearing"

    @SemanticUiCssMarker
    inline val top: SemanticTag get() = this + "top"

    @SemanticUiCssMarker
    inline val bottom: SemanticTag get() = this + "bottom"

    @SemanticUiCssMarker
    inline val bordered: SemanticTag get() = this + "bordered"

    @SemanticUiCssMarker
    inline val avatar: SemanticTag get() = this + "avatar"

    @SemanticUiCssMarker
    inline val placeholder: SemanticTag get() = this + "placeholder"

    @SemanticUiCssMarker
    inline val cards: SemanticTag get() = this + "cards"

    @SemanticUiCssMarker
    inline val card: SemanticTag get() = this + "card"

    @SemanticUiCssMarker
    inline val raised: SemanticTag get() = this + "raised"

    @SemanticUiCssMarker
    inline val meta: SemanticTag get() = this + "meta"

    @SemanticUiCssMarker
    inline val dropdown: SemanticTag get() = this + "dropdown"

    @SemanticUiCssMarker
    inline val sticky: SemanticTag get() = this + "sticky"

    @SemanticUiCssMarker
    inline val inline: SemanticTag get() = this + "inline"

    @SemanticUiCssMarker
    inline val progress: SemanticTag get() = this + "progress"

    @SemanticUiCssMarker
    inline val paragraph: SemanticTag get() = this + "paragraph"

    @SemanticUiCssMarker
    inline val slider: SemanticTag get() = this + "slider"

    @SemanticUiCssMarker
    inline val success: SemanticTag get() = this + "success"

    @SemanticUiCssMarker
    inline val ordered: SemanticTag get() = this + "ordered"

    @SemanticUiCssMarker
    inline val steps: SemanticTag get() = this + "steps"

    @SemanticUiCssMarker
    inline val step: SemanticTag get() = this + "step"

    @SemanticUiCssMarker
    inline val completed: SemanticTag get() = this + "completed"

    @SemanticUiCssMarker
    inline val computer: SemanticTag get() = this + "computer"

    @SemanticUiCssMarker
    inline val tablet: SemanticTag get() = this + "tablet"

    @SemanticUiCssMarker
    inline val mobile: SemanticTag get() = this + "mobile"

    @SemanticUiCssMarker
    inline val only: SemanticTag get() = this + "only"

    @SemanticUiCssMarker
    inline val form: SemanticTag get() = this + "form"

    @SemanticUiCssMarker
    inline val field: SemanticTag get() = this + "field"

    @SemanticUiCssMarker
    inline val fields: SemanticTag get() = this + "fields"

    @SemanticUiCssMarker
    inline val checkbox: SemanticTag get() = this + "checkbox"

    @SemanticUiCssMarker
    inline val error: SemanticTag get() = this + "error"

    @SemanticUiCssMarker
    inline val input: SemanticTag get() = this + "input"

    @SemanticUiCssMarker
    inline val sortable: SemanticTag get() = this + "sortable"

    @SemanticUiCssMarker
    inline val table: SemanticTag get() = this + "table"

    @SemanticUiCssMarker
    inline val toast: SemanticTag get() = this + "toast"

    @SemanticUiCssMarker
    inline val toastBox: SemanticTag get() = this + "toast-box"

    @SemanticUiCssMarker
    inline val toastContainer: SemanticTag get() = this + "toast-container"

    @SemanticUiCssMarker
    inline val container: SemanticTag get() = this + "container"

    @SemanticUiCssMarker
    inline val segment: SemanticTag get() = this + "segment"

    @SemanticUiCssMarker
    inline val segments: SemanticTag get() = this + "segments"

    @SemanticUiCssMarker
    inline val pusher: SemanticTag get() = this + "pusher"

    @SemanticUiCssMarker
    inline val left: SemanticTag get() = this + "left"

    @SemanticUiCssMarker
    inline val right: SemanticTag get() = this + "right"

    @SemanticUiCssMarker
    inline val ribbon: SemanticTag get() = this + "ribbon"

    @SemanticUiCssMarker
    inline val horizontal: SemanticTag get() = this + "horizontal"

    @SemanticUiCssMarker
    inline val equal: SemanticTag get() = this + "equal"

    @SemanticUiCssMarker
    inline val width: SemanticTag get() = this + "width"

    @SemanticUiCssMarker
    inline val vertical: SemanticTag get() = this + "vertical"

    @SemanticUiCssMarker
    inline val row: SemanticTag get() = this + "row"

    @SemanticUiCssMarker
    inline val column: SemanticTag get() = this + "column"

    @SemanticUiCssMarker
    inline val grid: SemanticTag get() = this + "grid"

    @SemanticUiCssMarker
    inline val wide: SemanticTag get() = this + "wide"

    @SemanticUiCssMarker
    inline val doubling: SemanticTag get() = this + "doubling"

    @SemanticUiCssMarker
    inline val stackable: SemanticTag get() = this + "stackable"

    @SemanticUiCssMarker
    inline val stretched: SemanticTag get() = this + "stretched"

    @SemanticUiCssMarker
    inline val aligned: SemanticTag get() = this + "aligned"

    @SemanticUiCssMarker
    inline val fitted: SemanticTag get() = this + "fitted"

    @SemanticUiCssMarker
    inline val center: SemanticTag get() = this + "center"

    @SemanticUiCssMarker
    inline val centered: SemanticTag get() = this + "centered"

    @SemanticUiCssMarker
    inline val divided: SemanticTag get() = this + "divided"

    @SemanticUiCssMarker
    inline val striped: SemanticTag get() = this + "striped"

    @SemanticUiCssMarker
    inline val celled: SemanticTag get() = this + "celled"

    @SemanticUiCssMarker
    inline val selectable: SemanticTag get() = this + "selectable"

    @SemanticUiCssMarker
    inline val middle: SemanticTag get() = this + "middle"

    @SemanticUiCssMarker
    inline val padded: SemanticTag get() = this + "padded"

    @SemanticUiCssMarker
    inline val pagination: SemanticTag get() = this + "pagination"

    @SemanticUiCssMarker
    inline val animated: SemanticTag get() = this + "animated"

    @SemanticUiCssMarker
    inline val animating: SemanticTag get() = this + "animating"

    @SemanticUiCssMarker
    inline val fade: SemanticTag get() = this + "fade"

    @SemanticUiCssMarker
    inline val out: SemanticTag get() = this + "out"

    @SemanticUiCssMarker
    inline val _in: SemanticTag get() = this + "in"

    @SemanticUiCssMarker
    inline val extra: SemanticTag get() = this + "extra"

    @SemanticUiCssMarker
    inline val very: SemanticTag get() = this + "very"

    @SemanticUiCssMarker
    inline val content: SemanticTag get() = this + "content"

    @SemanticUiCssMarker
    inline val scrolling: SemanticTag get() = this + "scrolling"

    @SemanticUiCssMarker
    inline val visible: SemanticTag get() = this + "visible"

    @SemanticUiCssMarker
    inline val hidden: SemanticTag get() = this + "hidden"

    @SemanticUiCssMarker
    inline val label: SemanticTag get() = this + "label"

    @SemanticUiCssMarker
    inline val tag: SemanticTag get() = this + "tag"

    @SemanticUiCssMarker
    inline val labeled: SemanticTag get() = this + "labeled"

    @SemanticUiCssMarker
    inline val menu: SemanticTag get() = this + "menu"

    @SemanticUiCssMarker
    inline val tab: SemanticTag get() = this + "tab"

    @SemanticUiCssMarker
    inline val tabular: SemanticTag get() = this + "tabular"

    @SemanticUiCssMarker
    inline val sidebar: SemanticTag get() = this + "sidebar"

    @SemanticUiCssMarker
    inline val item: SemanticTag get() = this + "item"

    @SemanticUiCssMarker
    inline val items: SemanticTag get() = this + "items"

    @SemanticUiCssMarker
    inline val dimmer: SemanticTag get() = this + "dimmer"

    @SemanticUiCssMarker
    inline val fullscreen: SemanticTag get() = this + "fullscreen"

    @SemanticUiCssMarker
    inline val modal: SemanticTag get() = this + "modal"

    @SemanticUiCssMarker
    inline val modals: SemanticTag get() = this + "modals"

    @SemanticUiCssMarker
    inline val page: SemanticTag get() = this + "page"

    @SemanticUiCssMarker
    inline val front: SemanticTag get() = this + "front"

    @SemanticUiCssMarker
    inline val selection: SemanticTag get() = this + "selection"

    @SemanticUiCssMarker
    inline val default: SemanticTag get() = this + "default"

    @SemanticUiCssMarker
    inline val selected: SemanticTag get() = this + "selected"

    @SemanticUiCssMarker
    inline val search: SemanticTag get() = this + "search"

    @SemanticUiCssMarker
    inline val popup: SemanticTag get() = this + "popup"

    @SemanticUiCssMarker
    inline val wireframe: SemanticTag get() = this + "wireframe"

    @SemanticUiCssMarker
    inline val transparent: SemanticTag get() = this + "transparent"

    @SemanticUiCssMarker
    inline val logo: SemanticTag get() = this + "logo"
}
