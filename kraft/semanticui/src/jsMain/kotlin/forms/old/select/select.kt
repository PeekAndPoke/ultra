package de.peekandpoke.kraft.semanticui.forms.old.select

import de.peekandpoke.kraft.components.comp
import kotlinx.html.Tag
import kotlin.reflect.KMutableProperty0

fun SelectFieldComponent.Config<String>.option(realValue: String) =
    option(realValue = realValue, formValue = realValue) { +realValue }

@Suppress("FunctionName")
fun <T : Any?> Tag.SelectField(
    value: T,
    onChange: (T) -> Unit,
    configure: SelectFieldComponent.Config<T>.() -> Unit = {},
) =
    SelectFieldComponent.Config(
        value = value, onChange = onChange,
    ).apply(configure)
        .let { config -> comp(config.asProps) { ctx -> SelectFieldComponent(ctx) } }

@Suppress("FunctionName")
fun <T> Tag.SelectField(
    property: KMutableProperty0<T>,
    configure: SelectFieldComponent.Config<T>.() -> Unit = {},
) =
    SelectField(property.get(), property::set, configure)
