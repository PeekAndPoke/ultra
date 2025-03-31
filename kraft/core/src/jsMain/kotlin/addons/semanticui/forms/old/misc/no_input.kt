package de.peekandpoke.kraft.addons.semanticui.forms.old.misc

import de.peekandpoke.kraft.components.comp
import kotlinx.html.Tag
import kotlin.reflect.KMutableProperty0

@Suppress("FunctionName")
fun <T> Tag.NoInputField(prop: KMutableProperty0<T>, configure: NoInputFieldComponent.Config<T>.() -> Unit = {}) =
    NoInputField(value = prop.get(), configure)

@Suppress("FunctionName")
fun Tag.NoInputField(
    configure: NoInputFieldComponent.Config<Unit>.() -> Unit = {},
) = NoInputField(value = Unit, configure)

@Suppress("FunctionName")
fun <T> Tag.NoInputField(
    value: T,
    configure: NoInputFieldComponent.Config<T>.() -> Unit = {},
) =
    NoInputFieldComponent.Config(
        value = value,
    ).apply(configure)
        .let { config -> comp(config.asProps) { ctx -> NoInputFieldComponent(ctx) } }
