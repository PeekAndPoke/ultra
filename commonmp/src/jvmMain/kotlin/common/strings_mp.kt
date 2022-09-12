package de.peekandpoke.ultra.common

import java.net.URLDecoder
import java.net.URLEncoder

actual fun String.encodeUriComponent(): String {
    return URLEncoder.encode(this, "UTF-8").replace("+", "%20")
}

actual fun String.decodeUriComponent(): String {
    return URLDecoder.decode(this, "UTF-8")
}
