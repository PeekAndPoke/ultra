package de.peekandpoke.mutator.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

fun KSPropertyDeclaration.isPrimaryCtorParameter(): Boolean {
    // Get the containing class
    val classDeclaration = parentDeclaration as? KSClassDeclaration ?: return false

    // Get the primary constructor
    val primaryConstructor = classDeclaration.primaryConstructor ?: return false

    // Check if this property name matches any of the constructor parameters
    return primaryConstructor.parameters.any { param ->
        param.name?.asString() == this.simpleName.asString()
    }
}

fun KSClassDeclaration.isData(): Boolean = modifiers.contains(Modifier.DATA)

fun KSClassDeclaration.isSealed(): Boolean = modifiers.contains(Modifier.SEALED)

fun KSClassDeclaration.isAbstract(): Boolean = modifiers.contains(Modifier.ABSTRACT)

fun KSDeclaration.getSimpleNames(): List<KSName> {
    val names = mutableListOf<KSName>()
    var current: KSDeclaration? = this

    while (current != null) {
        names.add(0, current.simpleName)
        current = current.parentDeclaration as? KSClassDeclaration
    }

    return names.toList()
}

fun KSDeclaration.isPrimitive(): Boolean {
    val qualifiedName = this.qualifiedName?.asString() ?: return false

    return when (qualifiedName) {
        Byte::class.qualifiedName,
        Short::class.qualifiedName,
        Int::class.qualifiedName,
        Long::class.qualifiedName,
        Float::class.qualifiedName,
        Double::class.qualifiedName,
        Boolean::class.qualifiedName,
        Char::class.qualifiedName,
            -> true

        else -> false
    }
}

fun KSDeclaration.isPrimitiveOrString(): Boolean {
    val qualifiedName = this.qualifiedName?.asString() ?: return false

    return isPrimitive() || when (qualifiedName) {
        CharSequence::class.qualifiedName,
        String::class.qualifiedName,
            -> true

        else -> false
    }
}

fun KSDeclaration.declaresKotlinList(): Boolean {
    return this is KSClassDeclaration &&
            this.qualifiedName?.asString() == List::class.qualifiedName!!
}

fun KSDeclaration.declaresKotlinSet(): Boolean {
    return this is KSClassDeclaration &&
            this.qualifiedName?.asString() == Set::class.qualifiedName!!
}
