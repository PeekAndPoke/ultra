package de.peekandpoke.ktorfx.rest.codegen.dart.addons

import de.peekandpoke.ktorfx.rest.codegen.dart.DartFile
import de.peekandpoke.ktorfx.rest.codegen.dart.addImport

fun DartFile.Definition.importDartConvert() {
    addImport("dart:convert")
}

