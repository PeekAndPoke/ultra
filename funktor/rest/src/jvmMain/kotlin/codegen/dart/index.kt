package de.peekandpoke.ktorfx.rest.codegen.dart

import de.peekandpoke.ktorfx.rest.codegen.dart.printer.DartCodePrinter
import de.peekandpoke.ktorfx.rest.docs.Docs
import kotlin.reflect.KParameter

interface DartPrintable {
    fun print(printer: DartCodePrinter): Any?
}

@Suppress("EnumEntryName")
enum class DartModifier {
    abstract,
    final,
    late,
    `interface`,
    static,
}

fun KParameter.extractDeprecationAnnotation(): DartAnnotation.Definition? {

    val deprecations = annotations.filterIsInstance<Docs.Deprecated>()

    if (deprecations.isEmpty()) {
        return null
    }

    val content = deprecations
        .joinToString(", ") { "Deprecated in Api-Version ${it.inVersion}: ${it.comment}" }
        .replace("'", "\\'")

    return DartAnnotation.Definition("@Deprecated('$content')")
}
