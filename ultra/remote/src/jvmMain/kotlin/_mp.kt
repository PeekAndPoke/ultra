package io.peekandpoke.ultra.remote

import java.net.URLEncoder

actual fun encodeURIComponent(value: String): String {
    return URLEncoder.encode(value, "utf-8")
}
