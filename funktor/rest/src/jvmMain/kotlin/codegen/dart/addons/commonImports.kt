package io.peekandpoke.funktor.rest.codegen.dart.addons

import io.peekandpoke.funktor.rest.codegen.dart.DartFile
import io.peekandpoke.funktor.rest.codegen.dart.addImport

fun DartFile.Definition.importDartConvert() {
    addImport("dart:convert")
}
