package de.peekandpoke.kraft.utils

import kotlinx.html.HTMLTag
import kotlinx.html.unsafe
import org.intellij.lang.annotations.Language

@Suppress("NOTHING_TO_INLINE")
inline fun HTMLTag.unsafeCss(@Language("css") css: String) {
    unsafe {
        +css
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun HTMLTag.unsafeJavascript(@Language("javascript") css: String) {
    unsafe {
        +css
    }
}
