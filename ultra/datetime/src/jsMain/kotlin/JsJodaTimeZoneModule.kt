package io.peekandpoke.ultra.datetime.kotlinx

@JsModule("@js-joda/timezone")
@JsNonModule
external object JsJodaTimeZoneModule

@Suppress("unused")
internal val jsJodaTz = JsJodaTimeZoneModule

fun initializeJsJodaTimezones(): JsJodaTimeZoneModule {
    return jsJodaTz
}
