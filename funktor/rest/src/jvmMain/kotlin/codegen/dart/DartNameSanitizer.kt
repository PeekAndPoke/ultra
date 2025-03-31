package de.peekandpoke.funktor.rest.codegen.dart

object DartNameSanitizer {

    private val reservedKeywords = listOf(
        "default",
        "is",
    )

    fun String.sanitizeVariableName() = if (this in reservedKeywords) {
        this + "_"
    } else {
        this
    }
}
