package de.peekandpoke.ultra.common.remote

import java.net.URLEncoder

actual fun encodeURIComponent(value: String): String {
    return URLEncoder.encode(value, "utf-8")
}
