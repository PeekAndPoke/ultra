package de.peekandpoke.funktor.rest.codegen.dart.addons

import de.peekandpoke.funktor.rest.codegen.dart.DartFile
import de.peekandpoke.funktor.rest.codegen.dart.addImport

fun DartFile.Definition.importDartConvert() {
    addImport("dart:convert")
}

