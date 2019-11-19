package de.peekandpoke.ultra.meta

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import de.peekandpoke.ultra.common.ucFirst
import java.util.*
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

@Suppress("unused")
interface ProcessorUtils {

    class Context(val logPrefix: String, val env: ProcessingEnvironment)

    val ctx: Context

    val env: ProcessingEnvironment get() = ctx.env

    val options: Map<String, String> get() = ctx.env.options
    val messager: Messager get() = ctx.env.messager
    val filer: Filer get() = ctx.env.filer
    val elementUtils: Elements get() = ctx.env.elementUtils
    val typeUtils: Types get() = ctx.env.typeUtils
    val sourceVersion: SourceVersion get() = ctx.env.sourceVersion
    val locale: Locale get() = ctx.env.locale

    fun logNote(str: String) =
        messager.printMessage(Diagnostic.Kind.NOTE, "${ctx.logPrefix} $str")

    fun logWarning(str: String) =
        messager.printMessage(Diagnostic.Kind.WARNING, "${ctx.logPrefix} $str")

    fun logError(str: String) =
        messager.printMessage(Diagnostic.Kind.ERROR, "${ctx.logPrefix} $str")

    fun logMandatoryWarning(str: String) =
        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "${ctx.logPrefix} $str")

    fun logOther(str: String) =
        messager.printMessage(Diagnostic.Kind.OTHER, "${ctx.logPrefix} $str")

    ////  REFLECTION  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    fun String.asKotlinClassName(): String = when {

        this == "java.util.Date" -> this

        else -> this
            .replace("/", "")
            .replace("kotlin.jvm.functions", "kotlin")
            .replace("java.lang.Throwable", "kotlin.Throwable")
            .replace("java.lang.", "kotlin.")
            .replace("java.util.", "kotlin.collections.")
            .replace("kotlin.Integer", "kotlin.Int")
            .replace("kotlin.Character", "kotlin.Char")
            .replace("kotlin.Object", "kotlin.Any")
    }

    val String.isPrimitiveType
        get() = when (asKotlinClassName()) {
            "kotlin.Boolean",
            "kotlin.Char",
            "kotlin.Byte",
            "kotlin.Short",
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Float",
            "kotlin.Double",
            "kotlin.Void" -> true

            else -> false
        }

    val String.isStringType get() = this == "java.lang.String"

    val String.isAnyType get() = this == "java.lang.Object"

    val TypeName.fqn get() = this.toString()

    val TypeName.packageName
        get(): String {

            return when (this) {
                is ParameterizedTypeName -> this.rawType.packageName

                else -> {
                    val parts = fqn.split(".")

                    parts.take(parts.size - 1).joinToString(".")
                }
            }
        }

    val TypeName.isPrimitiveType get() = fqn.isPrimitiveType

    val TypeName.isStringType get() = fqn.isStringType

    val TypeName.isAnyType get() = fqn.isAnyType

    fun TypeName.asKotlinClassName() = fqn.asKotlinClassName()

    val TypeMirror.fqn get() = asTypeName().toString()

    /**
     * Get all variables of a type element
     */
    val TypeElement.variables
        get() = enclosedElements
            .filterIsInstance<VariableElement>()
            .filter { !it.simpleName.contentEquals("Companion") }

    val TypeElement.methods
        get() = enclosedElements
            .filterIsInstance<ExecutableElement>()

    fun TypeElement.hasPublicGetterFor(v: VariableElement) =
        methods.any {
            it.simpleName.toString() == "get${v.simpleName.toString().ucFirst()}" &&
                    it.modifiers.contains(Modifier.PUBLIC)
        }

    val Element.fqn get() = asType().fqn

    val Element.isPrimitiveType get() = fqn.isPrimitiveType

    val Element.isStringType get() = fqn.isStringType

    val Element.isNullable get() = getAnnotation(org.jetbrains.annotations.Nullable::class.java) != null

    fun Element.asTypeName() = asType().asTypeName()

    /**
     * Get all types, that are directly or recursively used within the given Element
     */
    fun Element.getReferencedTypesRecursive(): Set<TypeMirror> {

        var last = mutableSetOf<TypeMirror>()
        var current = mutableSetOf(this.asType())

        while (current.size > last.size) {

            last = current

            current = last
                .map { it.getReferencedTypes() }
                .flatten()
                .distinct()
                .toMutableSet()
        }

        return current
    }

    /**
     * Get all types that are directly referenced by the given type
     */
    fun TypeMirror.getReferencedTypes(): Set<TypeMirror> {

        val result = mutableSetOf<TypeMirror>()

        if (this is DeclaredType) {

//            logNote("DeclaredType $this")

            // add the type it-self
            result.add(this)
            // add all generic type parameters
            result.addAll(this.typeArguments)

            // Next lets have a look at all the variables that the type defines
            val element = typeUtils.asElement(this)

            if (element is TypeElement) {

//                logNote("  .. TypeElement ${this}")

                val nested = element.variables.asSequence()
                    .filter { !it.asType().kind.isPrimitive }
                    .map { v: VariableElement ->

                        mutableListOf<TypeMirror>().apply {

                            val elType: TypeMirror = v.asType()

                            if (elType is DeclaredType) {
                                // add the type it self
                                add(elType)

//                                logNote("  .. ${v.simpleName} .. DeclaredType $elType")

                                // add all generic type arguments of the type that are declared types
                                addAll(elType.typeArguments.mapNotNull { (it as? DeclaredType) })
                            }
                        }
                    }
                    .flatten()
                    // Black-list of packages that we will not create code for
                    .filter { !it.fqn.startsWith("java.") }
                    .filter { !it.fqn.startsWith("javax.") }
                    .filter { !it.fqn.startsWith("kotlin.") }

                result.addAll(nested)
            }
        }

        return result
    }

    ////  KOTLIN COMPAT  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    fun Element.asKotlinClassName(): String = asType().asTypeName().toString().asKotlinClassName()

    fun ParameterizedTypeName.asRawKotlinClassName(): String = rawType.canonicalName.asKotlinClassName()
}
