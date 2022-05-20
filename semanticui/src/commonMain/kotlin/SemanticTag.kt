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
import kotlinx.html.attributesMapOf
import kotlin.js.JsName

@Suppress("FunctionName", "PropertyName", "unused", "Detekt:TooManyFunctions", "Detekt:VariableNaming")
class SemanticTag(
    @PublishedApi internal val parent: FlowContent,
    @PublishedApi internal val cssClasses: MutableList<String>,
) {
    fun _cssClasses(): String = cssClasses.filter { it.isNotBlank() }.joinToString(" ")

    fun _renderAsTag(block: FlowContent.() -> Unit) {
        block(parent)
    }

    private fun attrs() = attributesMapOf("class", _cssClasses())

    @SemanticUiCssMarker @JsName("_i") operator fun invoke(block: DIV.() -> Unit): Unit = Div(block)

    @SemanticUiTagMarker @JsName("H1") infix fun H1(block: H1.() -> Unit): Unit =
        H1(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("H2") infix fun H2(block: H2.() -> Unit): Unit =
        H2(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("H3") infix fun H3(block: H3.() -> Unit): Unit =
        H3(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("H4") infix fun H4(block: H4.() -> Unit): Unit =
        H4(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("H5") infix fun H5(block: H5.() -> Unit): Unit =
        H5(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("H6") infix fun H6(block: H6.() -> Unit): Unit =
        H6(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("A") infix fun A(block: A.() -> Unit): Unit =
        A(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("B") infix fun B(block: B.() -> Unit): Unit =
        B(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("Button") infix fun Button(block: BUTTON.() -> Unit): Unit =
        BUTTON(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("Div") infix fun Div(block: DIV.() -> Unit): Unit =
        DIV(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("Form") infix fun Form(block: FORM.() -> Unit): Unit =
        FORM(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("I") infix fun I(block: I.() -> Unit): Unit =
        I(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("Input") infix fun Input(block: INPUT.() -> Unit): Unit =
        INPUT(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("Img") infix fun Img(block: IMG.() -> Unit): Unit =
        IMG(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("Label") infix fun Label(block: LABEL.() -> Unit): Unit =
        LABEL(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("P") infix fun P(block: P.() -> Unit): Unit =
        P(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("Span") infix fun Span(block: SPAN.() -> Unit): Unit =
        SPAN(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("Submit") infix fun Submit(block: BUTTON.() -> Unit): Unit =
        BUTTON(attrs(), parent.consumer).visitNoInline(block)

    @SemanticUiTagMarker @JsName("Table") infix fun Table(block: TABLE.() -> Unit): Unit =
        TABLE(attrs(), parent.consumer).visitNoInline(block)

    // Adding Css Classes //////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker @JsName("_p") operator fun plus(cls: String): SemanticTag = apply { cssClasses.add(cls) }

    @SemanticUiCssMarker @JsName("_p2") operator fun plus(classes: Array<out String>): SemanticTag =
        apply { cssClasses.addAll(classes) }

    @SemanticUiCssMarker inline fun with(block: SemanticTag.() -> SemanticTag): SemanticTag = this.run(block)

    @SemanticUiCssMarker @JsName("_w") fun with(vararg cls: String): SemanticTag = this + cls

    @SemanticUiCssMarker @JsName("_w2") fun with(vararg cls: String, flow: DIV.() -> Unit): Unit =
        (this + cls).invoke(flow)

    // Conditional classes

    @SemanticUiConditionalMarker @JsName("_g") fun given(
        condition: Boolean,
        action: SemanticTag.() -> SemanticTag,
    ): SemanticTag = when (condition) {
        false -> this
        else -> this.action()
    }

    @SemanticUiConditionalMarker @JsName("_gn") fun givenNot(
        condition: Boolean,
        action: SemanticTag.() -> SemanticTag,
    ): SemanticTag = given(!condition, action)

    @SemanticUiConditionalMarker inline val then: SemanticTag get() = this

    // SemanticUI Numbers //////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker @JsName("_n") fun number(int: Int): SemanticTag = number(SemanticNumber.of(int))

    @SemanticUiCssMarker @JsName("_n2") fun number(int: Int, flow: DIV.() -> Unit): Unit =
        number(SemanticNumber.of(int), flow)

    @SemanticUiCssMarker @JsName("_n3") fun number(number: SemanticNumber): SemanticTag = this + number.toString()

    @SemanticUiCssMarker @JsName("_n4") fun number(number: SemanticNumber, flow: DIV.() -> Unit): Unit =
        with(number.toString(), flow = flow)

    @SemanticUiCssMarker val one: SemanticTag @JsName(Fn.f0001) get() = this + "one"

    @SemanticUiCssMarker val two: SemanticTag @JsName(Fn.f0002) get() = this + "two"

    @SemanticUiCssMarker val three: SemanticTag @JsName(Fn.f0003) get() = this + "three"

    @SemanticUiCssMarker val four: SemanticTag @JsName(Fn.f0004) get() = this + "four"

    @SemanticUiCssMarker val five: SemanticTag @JsName(Fn.f0005) get() = this + "five"

    @SemanticUiCssMarker val six: SemanticTag @JsName(Fn.f0006) get() = this + "six"

    @SemanticUiCssMarker val seven: SemanticTag @JsName(Fn.f0007) get() = this + "seven"

    @SemanticUiCssMarker val eight: SemanticTag @JsName(Fn.f0008) get() = this + "eight"

    @SemanticUiCssMarker val nine: SemanticTag @JsName(Fn.f0009) get() = this + "nine"

    @SemanticUiCssMarker val ten: SemanticTag @JsName(Fn.f0010) get() = this + "ten"

    @SemanticUiCssMarker val eleven: SemanticTag @JsName(Fn.f0011) get() = this + "eleven"

    @SemanticUiCssMarker val twelve: SemanticTag @JsName(Fn.f0012) get() = this + "twelve"

    @SemanticUiCssMarker val thirteen: SemanticTag @JsName(Fn.f0013) get() = this + "thirteen"

    @SemanticUiCssMarker val fourteen: SemanticTag @JsName(Fn.f0014) get() = this + "fourteen"

    @SemanticUiCssMarker val fifteen: SemanticTag @JsName(Fn.f0015) get() = this + "fifteen"

    @SemanticUiCssMarker val sixteen: SemanticTag @JsName(Fn.f0016) get() = this + "sixteen"

    // SemanticUI Colors ///////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker @JsName(Fn.f0017) fun color(color: SemanticColor): SemanticTag = when {
        color.isSet -> with(color.toString())
        else -> this
    }

    @SemanticUiCssMarker @JsName(Fn.f0018) fun color(color: SemanticColor, flow: DIV.() -> Unit): Unit =
        with(color.toString(), flow = flow)

    @SemanticUiCssMarker val color: SemanticTag @JsName(Fn.f0019) get() = this + "color"

    @SemanticUiCssMarker val inverted: SemanticTag @JsName(Fn.f0020) get() = this + "inverted"

    @SemanticUiCssMarker val red: SemanticTag @JsName(Fn.f0021) get() = this + "red"

    @SemanticUiCssMarker val orange: SemanticTag @JsName(Fn.f0022) get() = this + "orange"

    @SemanticUiCssMarker val yellow: SemanticTag @JsName(Fn.f0023) get() = this + "yellow"

    @SemanticUiCssMarker val olive: SemanticTag @JsName(Fn.f0024) get() = this + "olive"

    @SemanticUiCssMarker val green: SemanticTag @JsName(Fn.f0025) get() = this + "green"

    @SemanticUiCssMarker val teal: SemanticTag @JsName(Fn.f0026) get() = this + "teal"

    @SemanticUiCssMarker val blue: SemanticTag @JsName(Fn.f0027) get() = this + "blue"

    @SemanticUiCssMarker val violet: SemanticTag @JsName(Fn.f0028) get() = this + "violet"

    @SemanticUiCssMarker val purple: SemanticTag @JsName(Fn.f0029) get() = this + "purple"

    @SemanticUiCssMarker val pink: SemanticTag @JsName(Fn.f0030) get() = this + "pink"

    @SemanticUiCssMarker val brown: SemanticTag @JsName(Fn.f0031) get() = this + "brown"

    @SemanticUiCssMarker val grey: SemanticTag @JsName(Fn.f0032) get() = this + "grey"

    @SemanticUiCssMarker val black: SemanticTag @JsName(Fn.f0033) get() = this + "black"

    @SemanticUiCssMarker val white: SemanticTag @JsName(Fn.f0034) get() = this + "white"

    // Sizes ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker val short: SemanticTag @JsName(Fn.f0035) get() = this + "short"

    @SemanticUiCssMarker val long: SemanticTag @JsName(Fn.f0036) get() = this + "long"

    @SemanticUiCssMarker val mini: SemanticTag @JsName(Fn.f0037) get() = this + "mini"

    @SemanticUiCssMarker val tiny: SemanticTag @JsName(Fn.f0038) get() = this + "tiny"

    @SemanticUiCssMarker val small: SemanticTag @JsName(Fn.f0039) get() = this + "small"

    @SemanticUiCssMarker val medium: SemanticTag @JsName(Fn.f0040) get() = this + "medium"

    @SemanticUiCssMarker val large: SemanticTag @JsName(Fn.f0041) get() = this + "large"

    @SemanticUiCssMarker val big: SemanticTag @JsName(Fn.f0042) get() = this + "big"

    @SemanticUiCssMarker val huge: SemanticTag @JsName(Fn.f0043) get() = this + "huge"

    @SemanticUiCssMarker val massive: SemanticTag @JsName(Fn.f0044) get() = this + "massive"

    // SemanticUI Emphasis /////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker val primary: SemanticTag @JsName(Fn.f0045) get() = this + "primary"

    @SemanticUiCssMarker val secondary: SemanticTag @JsName(Fn.f0046) get() = this + "secondary"

    @SemanticUiCssMarker val positive: SemanticTag @JsName(Fn.f0047) get() = this + "positive"

    @SemanticUiCssMarker val negative: SemanticTag @JsName(Fn.f0048) get() = this + "negative"

    @SemanticUiCssMarker val warning: SemanticTag @JsName(Fn.f0049) get() = this + "warning"

    // SemanticUI Brands

    @SemanticUiCssMarker val facebook: SemanticTag @JsName(Fn.f0050) get() = this + "facebook"

    @SemanticUiCssMarker val twitter: SemanticTag @JsName(Fn.f0051) get() = this + "twitter"

    @SemanticUiCssMarker val google_plus: SemanticTag @JsName(Fn.f0052) get() = this + "google plus"

    @SemanticUiCssMarker val linkedin: SemanticTag @JsName(Fn.f0053) get() = this + "linkedin"

    @SemanticUiCssMarker val instagram: SemanticTag @JsName(Fn.f0054) get() = this + "instagram"

    @SemanticUiCssMarker val youtube: SemanticTag @JsName(Fn.f0055) get() = this + "youtube"

    // Semantic UI Words

    @SemanticUiCssMarker val icons: SemanticTag @JsName(Fn.f0056) get() = this + "icons"

    @SemanticUiCssMarker val text: SemanticTag @JsName(Fn.f0057) get() = this + "text"

    @SemanticUiCssMarker val actions: SemanticTag @JsName(Fn.f0058) get() = this + "actions"

    @SemanticUiCssMarker val active: SemanticTag @JsName(Fn.f0059) get() = this + "active"

    @SemanticUiCssMarker val basic: SemanticTag @JsName(Fn.f0060) get() = this + "basic"

    @SemanticUiCssMarker val bar: SemanticTag @JsName(Fn.f0061) get() = this + "bar"

    @SemanticUiCssMarker val button: SemanticTag @JsName(Fn.f0062) get() = this + "button"

    @SemanticUiCssMarker val buttons: SemanticTag @JsName(Fn.f0063) get() = this + "buttons"

    @SemanticUiCssMarker val circular: SemanticTag @JsName(Fn.f0064) get() = this + "circular"

    @SemanticUiCssMarker val compact: SemanticTag @JsName(Fn.f0065) get() = this + "compact"

    @SemanticUiCssMarker val disabled: SemanticTag @JsName(Fn.f0066) get() = this + "disabled"

    @SemanticUiCssMarker val divider: SemanticTag @JsName(Fn.f0067) get() = this + "divider"

    @SemanticUiCssMarker val dividing: SemanticTag @JsName(Fn.f0068) get() = this + "dividing"

    @SemanticUiCssMarker val down: SemanticTag @JsName(Fn.f0069) get() = this + "down"

    @SemanticUiCssMarker val floated: SemanticTag @JsName(Fn.f0070) get() = this + "floated"

    @SemanticUiCssMarker val floating: SemanticTag @JsName(Fn.f0071) get() = this + "floating"

    @SemanticUiCssMarker val fluid: SemanticTag @JsName(Fn.f0072) get() = this + "fluid"

    @SemanticUiCssMarker val flowing: SemanticTag @JsName(Fn.f0073) get() = this + "flowing"

    @SemanticUiCssMarker val piled: SemanticTag @JsName(Fn.f0074) get() = this + "piled"

    @SemanticUiCssMarker val fixed: SemanticTag @JsName(Fn.f0075) get() = this + "fixed"

    @SemanticUiCssMarker val header: SemanticTag @JsName(Fn.f0076) get() = this + "header"

    @SemanticUiCssMarker val icon: SemanticTag @JsName(Fn.f0077) get() = this + "icon"

    @SemanticUiCssMarker val image: SemanticTag @JsName(Fn.f0078) get() = this + "image"

    @SemanticUiCssMarker val line: SemanticTag @JsName(Fn.f0079) get() = this + "line"

    @SemanticUiCssMarker val link: SemanticTag @JsName(Fn.f0080) get() = this + "link"

    @SemanticUiCssMarker val bulleted: SemanticTag @JsName(Fn.f0081) get() = this + "bulleted"

    @SemanticUiCssMarker val list: SemanticTag @JsName(Fn.f0082) get() = this + "list"

    @SemanticUiCssMarker val loader: SemanticTag @JsName(Fn.f0083) get() = this + "loader"

    @SemanticUiCssMarker val loading: SemanticTag @JsName(Fn.f0084) get() = this + "loading"

    @SemanticUiCssMarker val message: SemanticTag @JsName(Fn.f0085) get() = this + "message"

    @SemanticUiCssMarker val overlay: SemanticTag @JsName(Fn.f0086) get() = this + "overlay"

    @SemanticUiCssMarker val pointing: SemanticTag @JsName(Fn.f0087) get() = this + "pointing"

    @SemanticUiCssMarker val scale: SemanticTag @JsName(Fn.f0088) get() = this + "scale"

    @SemanticUiCssMarker val shrink: SemanticTag @JsName(Fn.f0089) get() = this + "shrink"

    @SemanticUiCssMarker val toggle: SemanticTag @JsName(Fn.f0090) get() = this + "toggle"

    @SemanticUiCssMarker val styled: SemanticTag @JsName(Fn.f0091) get() = this + "styled"

    @SemanticUiCssMarker val accordion: SemanticTag @JsName(Fn.f0092) get() = this + "accordion"

    @SemanticUiCssMarker val title: SemanticTag @JsName(Fn.f0093) get() = this + "title"

    @SemanticUiCssMarker val description: SemanticTag @JsName(Fn.f0094) get() = this + "description"

    @SemanticUiCssMarker val transition: SemanticTag @JsName(Fn.f0095) get() = this + "transition"

    @SemanticUiCssMarker val relaxed: SemanticTag @JsName(Fn.f0096) get() = this + "relaxed"

    @SemanticUiCssMarker val attached: SemanticTag @JsName(Fn.f0097) get() = this + "attached"

    @SemanticUiCssMarker val clearing: SemanticTag @JsName(Fn.f0098) get() = this + "clearing"

    @SemanticUiCssMarker val top: SemanticTag @JsName(Fn.f0099) get() = this + "top"

    @SemanticUiCssMarker val bottom: SemanticTag @JsName(Fn.f0100) get() = this + "bottom"

    @SemanticUiCssMarker val bordered: SemanticTag @JsName(Fn.f0101) get() = this + "bordered"

    @SemanticUiCssMarker val avatar: SemanticTag @JsName(Fn.f0102) get() = this + "avatar"

    @SemanticUiCssMarker val placeholder: SemanticTag @JsName(Fn.f0103) get() = this + "placeholder"

    @SemanticUiCssMarker val cards: SemanticTag @JsName(Fn.f0104) get() = this + "cards"

    @SemanticUiCssMarker val card: SemanticTag @JsName(Fn.f0105) get() = this + "card"

    @SemanticUiCssMarker val raised: SemanticTag @JsName(Fn.f0106) get() = this + "raised"

    @SemanticUiCssMarker val meta: SemanticTag @JsName(Fn.f0107) get() = this + "meta"

    @SemanticUiCssMarker val dropdown: SemanticTag @JsName(Fn.f0108) get() = this + "dropdown"

    @SemanticUiCssMarker val sticky: SemanticTag @JsName(Fn.f0109) get() = this + "sticky"

    @SemanticUiCssMarker val inline: SemanticTag @JsName(Fn.f0110) get() = this + "inline"

    @SemanticUiCssMarker val progress: SemanticTag @JsName(Fn.f0111) get() = this + "progress"

    @SemanticUiCssMarker val paragraph: SemanticTag @JsName(Fn.f0112) get() = this + "paragraph"

    @SemanticUiCssMarker val slider: SemanticTag @JsName(Fn.f0113) get() = this + "slider"

    @SemanticUiCssMarker val success: SemanticTag @JsName(Fn.f0114) get() = this + "success"

    @SemanticUiCssMarker val ordered: SemanticTag @JsName(Fn.f0115) get() = this + "ordered"

    @SemanticUiCssMarker val steps: SemanticTag @JsName(Fn.f0116) get() = this + "steps"

    @SemanticUiCssMarker val step: SemanticTag @JsName(Fn.f0117) get() = this + "step"

    @SemanticUiCssMarker val completed: SemanticTag @JsName(Fn.f0118) get() = this + "completed"

    @SemanticUiCssMarker val computer: SemanticTag @JsName(Fn.f0119) get() = this + "computer"

    @SemanticUiCssMarker val tablet: SemanticTag @JsName(Fn.f0120) get() = this + "tablet"

    @SemanticUiCssMarker val mobile: SemanticTag @JsName(Fn.f0121) get() = this + "mobile"

    @SemanticUiCssMarker val only: SemanticTag @JsName(Fn.f0122) get() = this + "only"

    @SemanticUiCssMarker val form: SemanticTag @JsName(Fn.f0123) get() = this + "form"

    @SemanticUiCssMarker val field: SemanticTag @JsName(Fn.f0124) get() = this + "field"

    @SemanticUiCssMarker val fields: SemanticTag @JsName(Fn.f0125) get() = this + "fields"

    @SemanticUiCssMarker val checkbox: SemanticTag @JsName(Fn.f0126) get() = this + "checkbox"

    @SemanticUiCssMarker val error: SemanticTag @JsName(Fn.f0127) get() = this + "error"

    @SemanticUiCssMarker val input: SemanticTag @JsName(Fn.f0128) get() = this + "input"

    @SemanticUiCssMarker val sortable: SemanticTag @JsName(Fn.f0129) get() = this + "sortable"

    @SemanticUiCssMarker val table: SemanticTag @JsName(Fn.f0130) get() = this + "table"

    @SemanticUiCssMarker val toast: SemanticTag @JsName(Fn.f0131) get() = this + "toast"

    @SemanticUiCssMarker val toastBox: SemanticTag @JsName(Fn.f0132) get() = this + "toast-box"

    @SemanticUiCssMarker val toastContainer: SemanticTag @JsName(Fn.f0133) get() = this + "toast-container"

    @SemanticUiCssMarker val container: SemanticTag @JsName(Fn.f0134) get() = this + "container"

    @SemanticUiCssMarker val segment: SemanticTag @JsName(Fn.f0135) get() = this + "segment"

    @SemanticUiCssMarker val segments: SemanticTag @JsName(Fn.f0136) get() = this + "segments"

    @SemanticUiCssMarker val pusher: SemanticTag @JsName(Fn.f0137) get() = this + "pusher"

    @SemanticUiCssMarker val left: SemanticTag @JsName(Fn.f0138) get() = this + "left"

    @SemanticUiCssMarker val right: SemanticTag @JsName(Fn.f0139) get() = this + "right"

    @SemanticUiCssMarker val ribbon: SemanticTag @JsName(Fn.f0140) get() = this + "ribbon"

    @SemanticUiCssMarker val horizontal: SemanticTag @JsName(Fn.f0141) get() = this + "horizontal"

    @SemanticUiCssMarker val equal: SemanticTag @JsName(Fn.f0142) get() = this + "equal"

    @SemanticUiCssMarker val width: SemanticTag @JsName(Fn.f0143) get() = this + "width"

    @SemanticUiCssMarker val vertical: SemanticTag @JsName(Fn.f0144) get() = this + "vertical"

    @SemanticUiCssMarker val row: SemanticTag @JsName(Fn.f0145) get() = this + "row"

    @SemanticUiCssMarker val column: SemanticTag @JsName(Fn.f0146) get() = this + "column"

    @SemanticUiCssMarker val grid: SemanticTag @JsName(Fn.f0147) get() = this + "grid"

    @SemanticUiCssMarker val wide: SemanticTag @JsName(Fn.f0148) get() = this + "wide"

    @SemanticUiCssMarker val doubling: SemanticTag @JsName(Fn.f0149) get() = this + "doubling"

    @SemanticUiCssMarker val stackable: SemanticTag @JsName(Fn.f0150) get() = this + "stackable"

    @SemanticUiCssMarker val stretched: SemanticTag @JsName(Fn.f0151) get() = this + "stretched"

    @SemanticUiCssMarker val aligned: SemanticTag @JsName(Fn.f0152) get() = this + "aligned"

    @SemanticUiCssMarker val fitted: SemanticTag @JsName(Fn.f0153) get() = this + "fitted"

    @SemanticUiCssMarker val center: SemanticTag @JsName(Fn.f0154) get() = this + "center"

    @SemanticUiCssMarker val centered: SemanticTag @JsName(Fn.f0155) get() = this + "centered"

    @SemanticUiCssMarker val divided: SemanticTag @JsName(Fn.f0156) get() = this + "divided"

    @SemanticUiCssMarker val striped: SemanticTag @JsName(Fn.f0157) get() = this + "striped"

    @SemanticUiCssMarker val celled: SemanticTag @JsName(Fn.f0158) get() = this + "celled"

    @SemanticUiCssMarker val selectable: SemanticTag @JsName(Fn.f0159) get() = this + "selectable"

    @SemanticUiCssMarker val middle: SemanticTag @JsName(Fn.f0160) get() = this + "middle"

    @SemanticUiCssMarker val padded: SemanticTag @JsName(Fn.f0161) get() = this + "padded"

    @SemanticUiCssMarker val pagination: SemanticTag @JsName(Fn.f0162) get() = this + "pagination"

    @SemanticUiCssMarker val animated: SemanticTag @JsName(Fn.f0163) get() = this + "animated"

    @SemanticUiCssMarker val animating: SemanticTag @JsName(Fn.f0164) get() = this + "animating"

    @SemanticUiCssMarker val fade: SemanticTag @JsName(Fn.f0165) get() = this + "fade"

    @SemanticUiCssMarker val out: SemanticTag @JsName(Fn.f0166) get() = this + "out"

    @SemanticUiCssMarker val _in: SemanticTag @JsName(Fn.f0167) get() = this + "in"

    @SemanticUiCssMarker val extra: SemanticTag @JsName(Fn.f0168) get() = this + "extra"

    @SemanticUiCssMarker val very: SemanticTag @JsName(Fn.f0169) get() = this + "very"

    @SemanticUiCssMarker val content: SemanticTag @JsName(Fn.f0170) get() = this + "content"

    @SemanticUiCssMarker val scrolling: SemanticTag @JsName(Fn.f0171) get() = this + "scrolling"

    @SemanticUiCssMarker val visible: SemanticTag @JsName(Fn.f0172) get() = this + "visible"

    @SemanticUiCssMarker val hidden: SemanticTag @JsName(Fn.f0173) get() = this + "hidden"

    @SemanticUiCssMarker val label: SemanticTag @JsName(Fn.f0174) get() = this + "label"

    @SemanticUiCssMarker val tag: SemanticTag @JsName(Fn.f0175) get() = this + "tag"

    @SemanticUiCssMarker val labeled: SemanticTag @JsName(Fn.f0176) get() = this + "labeled"

    @SemanticUiCssMarker val menu: SemanticTag @JsName(Fn.f0177) get() = this + "menu"

    @SemanticUiCssMarker val tab: SemanticTag @JsName(Fn.f0178) get() = this + "tab"

    @SemanticUiCssMarker val tabular: SemanticTag @JsName(Fn.f0179) get() = this + "tabular"

    @SemanticUiCssMarker val sidebar: SemanticTag @JsName(Fn.f0180) get() = this + "sidebar"

    @SemanticUiCssMarker val item: SemanticTag @JsName(Fn.f0181) get() = this + "item"

    @SemanticUiCssMarker val items: SemanticTag @JsName(Fn.f0182) get() = this + "items"

    @SemanticUiCssMarker val dimmer: SemanticTag @JsName(Fn.f0183) get() = this + "dimmer"

    @SemanticUiCssMarker val fullscreen: SemanticTag @JsName(Fn.f0184) get() = this + "fullscreen"

    @SemanticUiCssMarker val modal: SemanticTag @JsName(Fn.f0185) get() = this + "modal"

    @SemanticUiCssMarker val modals: SemanticTag @JsName(Fn.f0186) get() = this + "modals"

    @SemanticUiCssMarker val page: SemanticTag @JsName(Fn.f0187) get() = this + "page"

    @SemanticUiCssMarker val front: SemanticTag @JsName(Fn.f0188) get() = this + "front"

    @SemanticUiCssMarker val selection: SemanticTag @JsName(Fn.f0189) get() = this + "selection"

    @SemanticUiCssMarker val default: SemanticTag @JsName(Fn.f0190) get() = this + "default"

    @SemanticUiCssMarker val selected: SemanticTag @JsName(Fn.f0191) get() = this + "selected"

    @SemanticUiCssMarker val search: SemanticTag @JsName(Fn.f0192) get() = this + "search"

    @SemanticUiCssMarker val popup: SemanticTag @JsName(Fn.f0193) get() = this + "popup"

    @SemanticUiCssMarker val wireframe: SemanticTag @JsName(Fn.f0194) get() = this + "wireframe"

    @SemanticUiCssMarker val transparent: SemanticTag @JsName(Fn.f0195) get() = this + "transparent"

    @SemanticUiCssMarker val logo: SemanticTag @JsName(Fn.f0196) get() = this + "logo"
}
