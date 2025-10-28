package de.peekandpoke.funktor.rest.codegen.dart.addons

import de.peekandpoke.funktor.rest.codegen.dart.DartClass
import de.peekandpoke.funktor.rest.codegen.dart.DartFile
import de.peekandpoke.funktor.rest.codegen.dart.DartMapType
import de.peekandpoke.funktor.rest.codegen.dart.DartNamedType
import de.peekandpoke.funktor.rest.codegen.dart.addAnnotation
import de.peekandpoke.funktor.rest.codegen.dart.addDoc
import de.peekandpoke.funktor.rest.codegen.dart.addFactoryFunction
import de.peekandpoke.funktor.rest.codegen.dart.addFunction
import de.peekandpoke.funktor.rest.codegen.dart.addImport
import de.peekandpoke.funktor.rest.codegen.dart.addParameter
import de.peekandpoke.funktor.rest.codegen.dart.addPartImport
import de.peekandpoke.funktor.rest.codegen.dart.body
import de.peekandpoke.funktor.rest.codegen.dart.returnType
import de.peekandpoke.ultra.slumber.builtin.polymorphism.PolymorphicChildUtil
import kotlinx.serialization.SerialName
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

fun DartFile.Definition.importJsonAnnotation() {
    addImport("package:json_annotation/json_annotation.dart")
}

fun DartClass.Definition.makeSerializable() {
    file.importJsonAnnotation()
    file.importJsonPart()
    addJsonSerializableAnnotation()
    addJsonFactory()
    addToJson()
}

fun DartFile.Definition.importJsonPart() {
    addPartImport("$fileName.g.dart")
}

fun DartFile.Definition.importMpDateTimePart() {
    addImport("package:${project.packageRoot}/converters/datetime-mp.dart")
}

fun DartClass.Definition.addJsonSerializableAnnotation() {

    file.importJsonAnnotation()
    file.importMpDateTimePart()

    when (typeParameters.isNotEmpty()) {
        true -> addAnnotation("@JsonSerializable(explicitToJson: true, converters: [InstantConverter()], genericArgumentFactories: true)")

        else -> addAnnotation("@JsonSerializable(explicitToJson: true, converters: [InstantConverter()])")
    }
}

fun DartClass.Definition.addToJson(discriminatorName: String = "_type") {

    val thisClass = tags.tags.filterIsInstance<KClass<*>>().firstOrNull()
    val serialName = thisClass?.findAnnotation<SerialName>()

    addFunction("toJson") {
        returnType(
            DartMapType.string2dynamic
        )

        typeParameters.forEach {
            addParameter(DartNamedType("Object? Function(${it.name} value)"), "toJson${it.name}")
        }

        body {
            if (serialName != null) {
                append("var map = _${"$"}${this@addToJson.name}ToJson(")
                join(listOf("this").plus(typeParameters.map { "toJson${it.name}" })) {
                    append(it)
                }
                append(");")
                nl()
                append("map['$discriminatorName'] = '${serialName.value}';").nl()
                nl()
                append("return map;")
            } else {
                append("return _${"$"}${this@addToJson.name}ToJson(")
                join(listOf("this").plus(typeParameters.map { "toJson${it.name}" })) {
                    append(it)
                }
                append(");")
            }
        }
    }
}

fun DartClass.Definition.addJsonFactory(discriminatorName: String = "_type") {

    val thisClass = tags.tags.filterIsInstance<KClass<*>>().firstOrNull()
    val allClasses = file.project.findAllClassDefinitions()

    val childClasses: List<Pair<String, KClass<*>>> = when {
        thisClass != null -> {
            allClasses.mapNotNull { cls ->

                val childClass = cls.tags.tags.filterIsInstance<KClass<*>>().firstOrNull()

                when {
                    childClass != null && childClass != thisClass && childClass.isSubclassOf(thisClass) -> {
                        // Get primary and additional serial names of the class
                        val ids = PolymorphicChildUtil.getAllIdentifiers(childClass)

                        ids
                    }

                    else -> null
                }
            }.flatten()
        }

        else -> emptyList()
    }

    addFactoryFunction("fromJson") {

        addParameter(DartMapType.string2dynamic, "json")

        typeParameters.forEach {
            addParameter(DartNamedType("${it.name} Function(Object? json)"), "fromJson${it.name}")
        }

        if (childClasses.isEmpty()) {
            body {
                append("return _${'$'}${this@addJsonFactory.name}FromJson(")
                join(listOf("json").plus(typeParameters.map { "fromJson${it.name}" })) {
                    append(it)
                }
                append(");")
            }
        } else {
            // make sure all child classes are imported
            childClasses.forEach { (_, cls) ->
                file.useClass(cls)
            }

            addDoc("Has ${childClasses.size} polymorphic child classes. Using type discriminator '${discriminatorName}'.")
            addDoc("Child classes are:")
            childClasses.forEachIndexed { index, it ->
                addDoc("  ${index + 1}. '${it.first}' to '${it.second.qualifiedName}'")
            }

            body {
                append("switch ('${"$"}{json['$discriminatorName']}') {").indent {

                    childClasses.forEach { (id, cls) ->
                        val imported = file.useClass(cls)
                        append("case '$id': ").indent {
                            append("return ").append(imported).append(".fromJson(json);")
                            nl()
                        }
                    }
                }
                append("}")
                nl()

                // Default clause
                append("return _${'$'}${this@addJsonFactory.name}FromJson(json);")
            }
        }
    }
}
