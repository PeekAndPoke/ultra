package de.peekandpoke.funktor.rest.codegen.dart

import de.peekandpoke.funktor.rest.codegen.CodeGenDsl
import de.peekandpoke.funktor.rest.codegen.Tags
import de.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrintFn
import de.peekandpoke.funktor.rest.codegen.dart.printer.DartCodePrinter
import de.peekandpoke.funktor.rest.codegen.dart.printer.appendDocBlock
import de.peekandpoke.funktor.rest.codegen.dart.printer.appendModifiers
import kotlin.reflect.KType

class DartClass(
    override val tags: Tags,
    override val file: DartFile.Definition,
    val name: String,
    val annotations: List<DartAnnotation>,
    val elements: List<DartClassElement>,
    val extends: DartClassType?,
    val implements: List<DartClassType>,
    val docBlock: String?,
    val modifiers: Set<DartModifier>,
    val typeParameters: List<DartNamedType>,
) : DartFileElement {

    @CodeGenDsl
    class Definition(
        override val file: DartFile.Definition,
        override val tags: Tags,
        val name: String,
        val builder: Definition.() -> Unit,
    ) : DartFileElement.Definition {

        /** List of doc blocks string added to the class */
        internal val docBlocks = mutableListOf<String>()

        /** List of annotations added to the class */
        internal val annotations = mutableListOf<DartAnnotation.Definition>()

        /** List of modifier added to the class definition */
        internal val modifiers = mutableSetOf<DartModifier>()

        /** List of elements added to the class */
        internal val elements = mutableListOf<DartClassElement.Definition>()

        /** Defines the super class */
        internal var extends: DartClassType? = null

        /** Defines the super class */
        internal var implements = mutableListOf<DartClassType>()

        /** List of generic type parameters */
        internal val typeParameters = mutableListOf<DartNamedType>()

        override fun implement() = apply(builder).run {
            DartClass(
                tags = tags,
                file = file,
                name = name,
                annotations = annotations.map { it.implement() },
                elements = elements.map { it.implement() },
                extends = extends,
                implements = implements,
                docBlock = docBlocks.joinToString(System.lineSeparator()),
                modifiers = modifiers,
                typeParameters = typeParameters.toList()
            )
        }

        fun getElements() = elements

        inline fun <reified T> filterElements() = getElements().filterIsInstance<T>()

        fun getAllProps() = filterElements<DartProperty.Definition>()

        fun isAbstract() = modifiers.contains(DartModifier.abstract)
    }

    override val info: String
        get() = name

    override fun print(printer: DartCodePrinter) = printer {

        appendDocBlock(docBlock)

        append(annotations) { nl() }

        appendModifiers(modifiers).append("class ").append(name)

        if (typeParameters.isNotEmpty()) {
            append("<").append(typeParameters) { append(", ") }.append(">")
        }

        extends?.let { append(" extends ").append(it) }

        if (implements.isNotEmpty()) {
            append(" implements ").append(implements) { append(", ") }
        }

        append(" {").indent {
            append(elements) { nl() }
        }
        append("}")

        nl()
    }
}

@CodeGenDsl
fun DartClass.Definition.addDoc(doc: String) {
    docBlocks.add(doc)
}

@CodeGenDsl
fun DartClass.Definition.extends(parent: DartClassType) {
    extends = parent
}

@CodeGenDsl
fun DartClass.Definition.implements(parent: DartClassType) {
    implements.add(parent)
}

@CodeGenDsl
fun DartClass.Definition.addAnnotation(annotation: DartAnnotation.Definition) {
    annotations.add(annotation)
}

@CodeGenDsl
fun DartClass.Definition.addAnnotation(complete: String) {
    addAnnotation(DartAnnotation.Definition(complete))
}

@CodeGenDsl
fun DartClass.Definition.abstract() {
    modifiers.add(DartModifier.abstract)
}

@CodeGenDsl
fun DartClass.Definition.addDefaultConstructor(
    tags: Tags = Tags.empty,
    body: DartCodePrintFn? = null,
) {
    elements.add(
        DartClassConstructor.Definition(
            tags = tags,
            classDef = this,
            body = body
        )
    )
}

@CodeGenDsl
fun DartClass.Definition.addConstructorWithNamedParameters() {
    elements.add(
        DartClassConstructorWithNamedParameters.Definition(
            classDef = this
        )
    )
}

@CodeGenDsl
fun DartClass.Definition.addCodeBlock(tags: Tags = Tags.empty, code: DartCodePrintFn) {
    elements.add(
        DartCodeBlock.Definition(this.file, tags, code)
    )
}

@CodeGenDsl
fun DartClass.Definition.addFunction(
    name: String,
    tags: Tags = Tags.empty,
    builder: DartFunction.Definition.() -> Unit,
) {
    elements.add(
        DartFunction.Definition(this.file, tags, name, builder)
    )
}

@CodeGenDsl
fun DartClass.Definition.addFunctionWithNamedParameters(
    name: String,
    tags: Tags = Tags.empty,
    builder: DartFunctionWithNamedParameters.Definition.() -> Unit,
) {
    elements.add(
        DartFunctionWithNamedParameters.Definition(this.file, tags, name, builder)
    )
}

@CodeGenDsl
fun DartClass.Definition.addFactoryFunction(
    name: String,
    tags: Tags = Tags.empty,
    builder: DartFactoryFunction.Definition.() -> Unit,
) {
    elements.add(
        DartFactoryFunction.Definition(this, tags, name, builder)
    )
}

@CodeGenDsl
fun DartClass.Definition.addProperty(
    type: KType,
    name: String,
    modifiers: Set<DartModifier> = emptySet(),
    tags: Tags = Tags.empty,
    builder: DartProperty.Definition.() -> Unit = {},
) {
    val resolved = file.resolveDartType(type)

    addProperty(
        type = resolved.type,
        name = name,
        modifiers = modifiers,
        tags = tags,
    ) {
        if (!resolved.successful) {
            addDoc("TODO: type '$type' was not resolved!")
        }
        builder()
    }
}

@CodeGenDsl
fun DartClass.Definition.addProperty(
    type: DartType,
    name: String,
    modifiers: Set<DartModifier> = emptySet(),
    tags: Tags = Tags.empty,
    builder: DartProperty.Definition.() -> Unit = {},
) {
    elements.add(
        DartProperty.Definition(this.file, tags, modifiers, name, type, builder)
    )
}

@CodeGenDsl
fun DartClass.Definition.addTypeParameter(type: DartNamedType) {
    typeParameters.add(type)
}

@CodeGenDsl
fun DartClass.Definition.addModifier(modifier: DartModifier) {
    modifiers.add(modifier)
}
