package de.peekandpoke.ktorfx.rest.codegen.dart

import de.peekandpoke.ktorfx.rest.codegen.dart.printer.DartCodePrinter

interface DartType : DartPrintable {
    val asNullable
        get(): NullableDartType = when (val x = this) {
            is NullableDartType -> x
            else -> NullableDartType(x)
        }
}

data class ResolvedDartType(val type: DartType, val successful: Boolean) {

    companion object {
        fun success(type: DartType) = ResolvedDartType(type, true)
        fun failed(type: DartType) = ResolvedDartType(type, false)
    }

    val failed = !successful
}

data class DartNamedType(val name: String) : DartType {
    override fun print(printer: DartCodePrinter) = printer {
        append(name)
    }
}

data class NullableDartType(val inner: DartType) : DartType {
    override fun print(printer: DartCodePrinter) = printer {
        inner.print(printer)
        append("?")
    }
}

val DartBoolean = DartNamedType("bool")

val DartDouble = DartNamedType("double")

val DartInt = DartNamedType("int")

val DartDynamic = DartNamedType("dynamic")

val DartString = DartNamedType("String")

val DartDate = DartNamedType("DateTime")

data class DartGenericType(val outer: DartType, val params: List<DartType>) : DartType {

    constructor(outer: DartType, inner: DartType) : this(outer, listOf(inner))

    override fun print(printer: DartCodePrinter) = printer {
        append(outer).append("<")
        append(params) { append(", ") }
        append(">")
    }
}

data class DartClassType(val cls: DartClass.Definition) : DartType {

    fun makeGeneric(params: List<DartType>) = DartGenericType(outer = this, params = params)

    override fun print(printer: DartCodePrinter) = printer {
        append(cls.name)
    }
}

data class DartTypedefType(val typedef: DartTypedef.Definition) : DartType {
    override fun print(printer: DartCodePrinter) = printer {
        append(typedef.name)
    }
}

data class DartEnumType(val enum: DartEnum.Definition) : DartType {
    override fun print(printer: DartCodePrinter) = printer {
        append(enum.name)
    }
}

data class DartListType(val inner: DartType) : DartType {
    override fun print(printer: DartCodePrinter) = printer {
        append("List<").append(inner).append(">")
    }
}

data class DartMapType(val key: DartType, val value: DartType) : DartType {

    companion object {
        val string2dynamic = DartMapType(DartString, DartDynamic)
    }

    override fun print(printer: DartCodePrinter) = printer {
        append("Map<").append(key).append(", ").append(value).append(">")
    }
}

data class DartSetType(val inner: DartType) : DartType {
    override fun print(printer: DartCodePrinter) = printer {
        append("Set<").append(inner).append(">")
    }
}

data class DartFuncType(val returns: DartType, val parameters: List<DartType>) : DartType {
    override fun print(printer: DartCodePrinter) = printer {
        append(returns).append(" Function(").append(parameters) { append(", ") }.append(")")
    }
}
