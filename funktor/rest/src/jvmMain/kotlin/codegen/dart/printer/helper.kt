package de.peekandpoke.funktor.rest.codegen.dart.printer

import de.peekandpoke.funktor.rest.codegen.dart.DartModifier

fun DartCodePrinter.appendDocBlock(doc: String?) = apply {

    doc?.let { doc ->

        if (doc.isNotBlank()) {
            doc.split(System.lineSeparator()).map { "/// $it" }.forEach { appendLine(it) }
        }
    }
}

fun DartCodePrinter.appendModifiers(modifiers: Set<DartModifier>) = apply {
    modifiers.forEach { append(it.toString()).append(" ") }
}
