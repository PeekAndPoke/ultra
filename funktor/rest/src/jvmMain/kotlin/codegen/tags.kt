package de.peekandpoke.funktor.rest.codegen

import kotlin.reflect.KClass

fun tagged(vararg tag: Any) = Tags(tag)

interface Taggable {
    val tags: Tags
}

class Tags(val tags: Array<out Any>) {

    companion object {
        val empty = Tags(emptyArray())
    }

    fun contains(tag: Any): Boolean {

        return tags.any {
            when {
                tag is KClass<*> && it is KClass<*> -> tag.qualifiedName == it.qualifiedName

                else -> tag == it
            }
        }
    }
}

