package de.peekandpoke.funktor.rest.codegen.dart.addons

import de.peekandpoke.funktor.rest.codegen.CodeGenDsl
import de.peekandpoke.funktor.rest.codegen.dart.DartAnnotation
import de.peekandpoke.funktor.rest.codegen.dart.DartClass
import de.peekandpoke.funktor.rest.codegen.dart.DartClassConstructor
import de.peekandpoke.funktor.rest.codegen.dart.DartClassType
import de.peekandpoke.funktor.rest.codegen.dart.DartFile
import de.peekandpoke.funktor.rest.codegen.dart.DartFunctionParameter
import de.peekandpoke.funktor.rest.codegen.dart.DartModifier
import de.peekandpoke.funktor.rest.codegen.dart.DartNameSanitizer.sanitizeVariableName
import de.peekandpoke.funktor.rest.codegen.dart.DartNamedType
import de.peekandpoke.funktor.rest.codegen.dart.DartProject
import de.peekandpoke.funktor.rest.codegen.dart.DartProperty
import de.peekandpoke.funktor.rest.codegen.dart.DartString
import de.peekandpoke.funktor.rest.codegen.dart.addAnnotation
import de.peekandpoke.funktor.rest.codegen.dart.addClass
import de.peekandpoke.funktor.rest.codegen.dart.addConstructorWithNamedParameters
import de.peekandpoke.funktor.rest.codegen.dart.addDoc
import de.peekandpoke.funktor.rest.codegen.dart.addEnum
import de.peekandpoke.funktor.rest.codegen.dart.addFunction
import de.peekandpoke.funktor.rest.codegen.dart.addImport
import de.peekandpoke.funktor.rest.codegen.dart.addModifier
import de.peekandpoke.funktor.rest.codegen.dart.addParameter
import de.peekandpoke.funktor.rest.codegen.dart.addProperty
import de.peekandpoke.funktor.rest.codegen.dart.addTypeParameter
import de.peekandpoke.funktor.rest.codegen.dart.body
import de.peekandpoke.funktor.rest.codegen.dart.extends
import de.peekandpoke.funktor.rest.codegen.dart.extractDeprecationAnnotation
import de.peekandpoke.funktor.rest.codegen.dart.implements
import de.peekandpoke.funktor.rest.codegen.dart.initialize
import de.peekandpoke.funktor.rest.codegen.dart.returnType
import de.peekandpoke.funktor.rest.codegen.joinSimpleNames
import de.peekandpoke.funktor.rest.codegen.tagged
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.datetime.MpLocalDate
import de.peekandpoke.ultra.common.datetime.MpLocalDateTime
import de.peekandpoke.ultra.common.datetime.MpLocalTime
import de.peekandpoke.ultra.common.datetime.MpZonedDateTime
import de.peekandpoke.ultra.common.ucFirst
import de.peekandpoke.ultra.slumber.Slumber
import kotlinx.serialization.SerialName
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.withNullability

@CodeGenDsl
fun DartProject.Definition.addSerializableClasses(builder: DartSerializableCodeGen.Builder.() -> Unit) {
    DartSerializableCodeGen.Builder(this)
        .apply(builder)
        .build()
        .run()
}

class DartSerializableCodeGen(
    val project: DartProject.Definition,
    val packageMapping: (String) -> String,
    val classes: Set<KType>,
) {
    class Builder(
        val project: DartProject.Definition,
    ) {
        private var packageMapping: (String) -> String = { it }
        private var excludeFilter: (KType) -> Boolean = { false }

        private val classes = mutableMapOf<String, KType>()

        fun build() = DartSerializableCodeGen(
            project = project,
            packageMapping = packageMapping,
            classes = classes.values
                .filter { !excludeFilter(it) }
                .toSet()
        )

        fun mapPackages(mapping: (String) -> String) {
            packageMapping = mapping
        }

        /** Set the exclude filter */
        fun exclude(filter: (KType) -> Boolean) {
            excludeFilter = filter
        }

        /** Add multiple types */
        fun addTypes(types: Iterable<KType>) {
            types.forEach(::addType)
        }

        /** Adds a class */
        fun addType(type: KType) {
            (type.classifier as? KClass<*>)?.let { classifier ->
                classes[classifier.qualifiedName!!] = type
            }
        }
    }

    fun run() {
        val groupedByFile = classes.groupBy { (it.classifier as KClass<*>).mapPackageToFile() }

        project.apply {
            groupedByFile.forEach { (path, types) ->
                file(path) {
                    types.forEach { type -> addType(type) }
                }
            }
        }
    }

    private fun KClass<*>.mapPackageToFile() = packageMapping(java.`package`.name).replace(".", "/") + ".dart"

    private fun DartFile.Definition.addType(type: KType) {
        val cls = type.classifier as KClass<*>

        when {
            cls.java.isEnum -> addEnum(cls)

            else -> addClass(type)
        }
    }

    private fun DartFile.Definition.addEnum(cls: KClass<*>) {

        addEnum(cls.joinSimpleNames(), tagged(cls)) {
            addImport("package:enum_annotation/enum_annotation.dart")
            importJsonPart()

            addValues(
                *cls.java.enumConstants.map { it.toString() }.toTypedArray()
            )
        }
    }

    private fun DartFile.Definition.addClass(type: KType) {

        val cls = type.classifier as KClass<*>

        if (cls.java.isInterface && !cls.isSealed) {
            return
        }

        addClass(cls.joinSimpleNames(), tagged(cls)) {
            val classDefinition = this

            file.importMpDateTimePart()

            val dartClass = import()

            applyExtends(cls)

            if (cls.isSealed) {
                addModifier(DartModifier.`interface`)
            }

            cls.typeParameters.forEach { kTypeParameter ->
                addTypeParameter(DartNamedType(kTypeParameter.name))
            }

            // NOTICE: This needs to be called after the type parameters where added
            makeSerializable()

            fun DartProperty.Definition.addAdditionalAnnotations(type: KType) {
                when (type.classifier) {
                    MpZonedDateTime::class -> {
                        when (type.isMarkedNullable) {
                            true -> addAnnotation("@ZonedDateTimeConverterNullable()")
                            else -> addAnnotation("@ZonedDateTimeConverter()")
                        }
                    }

                    MpInstant::class, MpLocalDateTime::class, MpLocalDate::class -> {
                        when (type.isMarkedNullable) {
                            true -> addAnnotation("@InstantConverterNullable()")
                            else -> addAnnotation("@InstantConverter()")
                        }
                    }

                    MpLocalTime::class -> {
                        when (type.isMarkedNullable) {
                            true -> addAnnotation("@LocalTimeConverterNullable()")
                            else -> addAnnotation("@LocalTimeConverter()")
                        }
                    }
                }
            }

            fun generateProperty(param: KParameter, name: String, isOverride: Boolean) {
                val paramType = param.type
                val cleanedName = name.removePrefix("_").sanitizeVariableName()
                val isOfTypeEnum = paramType.classifier is KClass<*> && (paramType.classifier as KClass<*>).java.isEnum

                when (isOfTypeEnum) {
                    true -> addProperty(
                        type = paramType.withNullability(true),
                        name = cleanedName,
                        modifiers = setOf(DartModifier.final),
                    ) {
                        if (cleanedName != name) {
                            addAnnotation("@JsonKey(name: '${name}')")
                        }

                        addAnnotation("@JsonKey(unknownEnumValue: JsonKey.nullForUndefinedEnumValue)")

                        param.extractDeprecationAnnotation()?.let { addAnnotation(it) }

                        // Additional Serializer Annotations
                        addAdditionalAnnotations(paramType)

                        if (isOverride) {
                            addAnnotation(DartAnnotation.override)
                        }
                    }

                    false -> addProperty(
                        type = paramType,
                        name = cleanedName,
                        modifiers = setOf(DartModifier.final),
                    ) {
                        if (cleanedName != name) {
                            addAnnotation("@JsonKey(name: '${name}')")
                        }

                        param.extractDeprecationAnnotation()?.let { addAnnotation(it) }

                        // Additional Serializer Annotations
                        addAdditionalAnnotations(paramType)

                        if (isOverride) {
                            addAnnotation(DartAnnotation.override)
                        }
                    }
                }
            }

            fun shouldGenerateProperty(host: KClass<*>, element: KAnnotatedElement): Boolean {
                return when {
                    host.isData -> true
                    host.isAbstract || host.isSealed -> element.annotations.filterIsInstance<Slumber.Field>()
                        .isNotEmpty()

                    else -> false
                }
            }

            when {
                cls.isData -> {

                    when (val serialName = cls.annotations.filterIsInstance<SerialName>().firstOrNull()) {
                        null -> Unit

                        else -> addProperty(
                            type = DartString,
                            name = "SERIAL_TYPE",
                            modifiers = setOf(DartModifier.static, DartModifier.final),
                        ) {
                            initialize { append("'${serialName.value}'") }
                        }
                    }

                    val parameters = (cls.primaryConstructor?.parameters ?: emptyList())

                    parameters.forEach { param ->

                        val isOverride = cls.allSuperclasses.any { superClass ->
                            superClass.memberProperties
                                .filter { superClassProp -> shouldGenerateProperty(superClass, superClassProp) }
                                .any { superClassProp -> superClassProp.name == param.name }
                        }

                        generateProperty(param, param.name!!, isOverride)
                    }
                }

                cls.isAbstract || cls.isSealed -> {
                    cls.declaredMemberProperties
                        .filter { property -> shouldGenerateProperty(cls, property) }
                        .map { property ->
                            val propertyCls = property.returnType.classifier
                            val propertyIsEnum = propertyCls is KClass<*> && propertyCls.java.isEnum

                            addProperty(
                                type = property.returnType.withNullability(property.returnType.isMarkedNullable || propertyIsEnum),
                                name = property.name,
                                modifiers = setOf(DartModifier.final),
                            ) {
                                addAdditionalAnnotations(property.returnType)
                            }
                        }
                }
            }

            addConstructorWithNamedParameters()

            // Create "withers"
            if (cls.isData) {
                fun DartFunctionParameter.cleanName() = name.removePrefix("_").sanitizeVariableName()

                DartClassConstructor.Parameters.of(classDefinition).all.forEach { param ->
                    val name = param.cleanName()

                    // create "with" methods
                    addFunction("with${name.ucFirst()}") {
                        returnType(dartClass)
                        addParameter(param.type, name)
                        addDoc("Returns a copy while updating [$name]")

                        body {
                            val params = DartClassConstructor.Parameters.of(classDefinition)

                            append("return ").append(dartClass).append("(").indent {
                                params.all.forEach {
                                    append(it.cleanName()).append(": ").append(it.cleanName()).append(",").nl()
                                }
                            }.append(");")
                        }
                    }
                }
            }
        }
    }

    private fun DartClass.Definition.applyExtends(cls: KClass<*>) {
//        val parentInterface = cls.supertypes.filter { (it.classifier as KClass<*>).java.isInterface }

        cls.supertypes.filter {
            val classifier = (it.classifier as KClass<*>)

            !classifier.java.isInterface || classifier.isSealed
        }.forEachIndexed { idx, it ->
            val resolvedParentType = file.resolveDartType(it)
            val parentDartType = resolvedParentType.type

            when (resolvedParentType.successful && parentDartType is DartClassType) {
                true -> if (idx == 0) extends(parentDartType) else implements(parentDartType)
                else -> addDoc("TODO: parent type '$it' could not be resolved.")
            }
        }
    }
}
