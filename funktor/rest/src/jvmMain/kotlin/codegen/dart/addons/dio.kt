package de.peekandpoke.funktor.rest.codegen.dart.addons

import de.peekandpoke.funktor.rest.codegen.dart.DartBoolean
import de.peekandpoke.funktor.rest.codegen.dart.DartClass
import de.peekandpoke.funktor.rest.codegen.dart.DartFile
import de.peekandpoke.funktor.rest.codegen.dart.DartFunctionWithNamedParameters
import de.peekandpoke.funktor.rest.codegen.dart.DartGenericType
import de.peekandpoke.funktor.rest.codegen.dart.DartMapType
import de.peekandpoke.funktor.rest.codegen.dart.DartModifier
import de.peekandpoke.funktor.rest.codegen.dart.DartNamedType
import de.peekandpoke.funktor.rest.codegen.dart.DartString
import de.peekandpoke.funktor.rest.codegen.dart.DartType
import de.peekandpoke.funktor.rest.codegen.dart.addClass
import de.peekandpoke.funktor.rest.codegen.dart.addCodeBlock
import de.peekandpoke.funktor.rest.codegen.dart.addDefaultConstructor
import de.peekandpoke.funktor.rest.codegen.dart.addDoc
import de.peekandpoke.funktor.rest.codegen.dart.addFunction
import de.peekandpoke.funktor.rest.codegen.dart.addFunctionWithNamedParameters
import de.peekandpoke.funktor.rest.codegen.dart.addImport
import de.peekandpoke.funktor.rest.codegen.dart.addParameter
import de.peekandpoke.funktor.rest.codegen.dart.addProperty
import de.peekandpoke.funktor.rest.codegen.dart.addTypeParameter
import de.peekandpoke.funktor.rest.codegen.dart.async
import de.peekandpoke.funktor.rest.codegen.dart.body
import de.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrintFn
import de.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrinter
import de.peekandpoke.funktor.rest.codegen.dart.printer.dartCodePrinterFn
import de.peekandpoke.funktor.rest.codegen.dart.returnType
import de.peekandpoke.funktor.rest.codegen.tagged
import kotlin.reflect.KClass
import kotlin.reflect.KType

fun DartFile.Definition.importDio() {
    addImport("package:dio/dio.dart")
}

fun DartFile.Definition.importDioApiResponseClasses() {
    project.findFirstClassByTag("dio:ApiResponse").import()
}

fun DartFile.Definition.createDioApiResponseClasses() {

    val typeParam = DartNamedType("T")

    importDio()

    addClass("ApiResponse", tagged("dio:ApiResponse")) {
        addTypeParameter(typeParam)

        addProperty(typeParam.asNullable, "data", setOf(DartModifier.late, DartModifier.final))

        addProperty(DartNamedType("Response"), "response", setOf(DartModifier.late, DartModifier.final))

        addDefaultConstructor()

        addFunction("isSuccess") {
            returnType = DartBoolean
            body {
                append("var status = response.statusCode;")
                nl()
                append("return status != null && status >= 200 && status < 299;")
            }
        }

        addCodeBlock {
            append("void handle({required Function(T) success, Function(Response)? error}) {").indent {
                append("if (isSuccess()) {").indent {
                    append("if (data != null) {").indent {
                        appendLine("success(data!);")
                    }
                    appendLine("}")
                }
                append("} else {").indent {
                    append("if (error != null) {").indent {
                        appendLine("error(response);")
                    }
                    appendLine("}")
                }
                appendLine("}")
            }
            appendLine("}")
        }
    }
}

fun DartFile.Definition.addDioApplyParamsHelper() {
    addFunctionWithNamedParameters("_applyParams") {
        returnType(DartString)
        addParameter(DartString, "uri")
        addParameter(DartMapType(DartString, DartString), "params")
        body(
            """
                var rest = <String>[];
                
                params.forEach((key, value) {
                
                  var valueEnc = Uri.encodeComponent(value);
                  var pattern = '{${"$"}key}';
                
                  if (uri.contains(pattern)) {
                    uri = uri.replaceAll(pattern, valueEnc);
                  } else {
                    rest.add(
                        '${"$"}key=${"$"}valueEnc'
                    );
                  }
                });
                
                if (rest.isEmpty) {
                  return uri;
                }
                
                return uri + '?' + rest.join('&');
            """.trimIndent()
        )
    }
}

fun DartClass.Definition.addDioEndpointFunction(
    /** The function name */
    name: String,
    /** The http method */
    httpMethod: String,
    /** The uri pattern */
    pattern: String,
    /** Params where the key is the param name and the value if the params is optional */
    params: List<Pair<String, Boolean>>,
    /** The type of the request body */
    bodyType: KType?,
    /** The return type of request */
    returnType: KType,
) {
    if ((returnType.classifier as? KClass<*>)?.simpleName != "ApiResponse") {
        return
    }

    file.importDartConvert()
    file.importDio()

    val bodyCls = bodyType?.classifier as? KClass<*>
    val bodyDartType = bodyCls?.let { file.useClass(bodyCls) }

    addFunctionWithNamedParameters(name) {
        async()

        val apiResponseType = returnType.arguments.first().type!!

        val dartReturnType: DartType = addResultType(apiResponseType)

        params.forEach { (name, optional) ->
            when (optional) {
                false -> addParameter(DartString, name)
                else -> addParameter(DartString, name) { append("''") }
            }
        }

        // When the request has a body, we create a parameter for it
        bodyDartType?.let { body ->
            addParameter(body, "requestBody")
        }

        body {
            appendLine("// ignore: unnecessary_this")

            append("var response = await dio().${httpMethod.lowercase()}(").indent {
                append("_applyParams(uri: '$pattern', params: {").indent {
                    // Add all request parameters
                    params.forEach { (name, _) ->
                        append("'").append(name).append("'").append(": ").append(name).append(",").nl()
                    }
                }.appendLine("}),")

                // When the request has a body we also add it to the Dio.xxx("...", data: ...)
                bodyDartType?.let {
                    append("data: requestBody.toJson(),").nl()
                }
            }.appendLine(");")

            nl()

            appendLine("var data = response.data is Map ? response.data as Map<String, dynamic> : null;")

            fun DartCodePrinter.appendFromJsonParams(dataParam: String, type: DartType) {
                when (type) {
                    is DartGenericType -> {
                        join(
                            listOf<DartCodePrintFn> { append(dataParam) }.plus(
                                type.params.map {
                                    dartCodePrinterFn {
                                        append("(it) => ").append(it).append(".fromJson(it as Map<String, dynamic>)")
                                    }
                                }
                            )
                        ) { it() }
                    }

                    else -> append(dataParam)
                }
            }

            when (apiResponseType.classifier) {
                List::class -> {
                    val innerResolved = file.resolveDartType(apiResponseType.arguments[0].type!!)

                    appendLine("var dataData = data != null ? data['data'] as List<dynamic>? : [];")
                    append("var content = dataData != null").indent {
                        append("? dataData.map((it) => ").append(innerResolved.type).append(".fromJson(")
                        appendFromJsonParams("it as Map<String, dynamic>", innerResolved.type)
                        append(")).toList()")
                        nl()
                        append(": <").append(innerResolved.type).append(">[];")
                        nl()
                    }
                }

                else -> {
                    appendLine("var dataData = data != null ? data['data'] as Map<String, dynamic>? : null;")
                    append("var content = dataData != null ? ").append(dartReturnType).append(".fromJson(")
                    appendFromJsonParams("dataData", dartReturnType)
                    append(") : null;")
                    nl()
                }
            }
            nl()

            append("return Future.value(").indent {
                appendLine("ApiResponse(content, response)")
            }.append(");")
        }
    }
}

private fun DartFunctionWithNamedParameters.Definition.addResultType(type: KType): DartType {

    // Make sure the dio classes are imported
    file.importDioApiResponseClasses()

    val resolved = file.resolveDartType(type)

    if (resolved.failed) {
        addDoc("Failed to resolve type '${type}'")
    } else {
        // Make sure the return type is imported
        (type.classifier as? KClass<*>)?.let { cls ->
            file.apply {
                project.findFirstClassByTagOrNull(cls)?.import()
            }
        }
    }

    returnType(
        DartGenericType(
            DartNamedType("Future"),
            DartGenericType(
                DartNamedType("ApiResponse"),
                resolved.type,
            )
        )
    )

    return resolved.type
}
