@file:Suppress("PackageDirectoryMismatch")

package org.intellij.lang.annotations

@Retention(AnnotationRetention.BINARY)
internal annotation class Language(
    val value: String,
    val prefix: String = "",
    val suffix: String = "",
)
