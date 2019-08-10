package de.peekandpoke.ultra.mutator

import javassist.ClassPool
import javassist.CtNewConstructor
import javassist.util.proxy.ProxyFactory
import org.atteo.classindex.ClassIndex
import org.atteo.classindex.IndexSubclasses
import java.lang.reflect.Modifier

@IndexSubclasses
interface MakeClassesProxiable {
    fun get(): List<String>
}

object Frozen {

    fun init() {
        manipulateAllClasses()
    }

    private fun manipulateAllClasses() {

        val classes = ClassIndex.getSubclasses(MakeClassesProxiable::class.java)

        val all = classes.map { it.newInstance() }.flatMap { it.get() }.distinct()

        all.forEach { manipulateClass(it) }
    }

    private fun manipulateClass(className: String): Class<*> {

        println("manipulating $className")

        val pool = ClassPool.getDefault()

        val cls = pool.get(className)

        val defaultConstructor = CtNewConstructor.make("public ${cls.simpleName}() {}", cls)
        cls.addConstructor(defaultConstructor)

        // clear the final modifier on the class
        cls.modifiers = cls.modifiers and Modifier.FINAL.inv()

        cls.declaredMethods.forEach {
            // clear the final modifier on all methods
            it.modifiers = it.modifiers and Modifier.FINAL.inv()
        }

        return cls.toClass()
    }

    inline fun <reified T> createLazyProxy(noinline creator: () -> T): T {

        val factory = ProxyFactory()

        factory.superclass = T::class.java

        factory.setFilter { true }

        var lazy: T? = null

        val proxy = factory.create(arrayOf(), arrayOf()) { self, thisMethod, proceed, args ->

            if (lazy == null) {
                lazy = creator()

                // Copy all field values to the proxy.
                // Why? We need this to make the data class copy() method work.

                // TODO: move this to Cloner and make use of kotlins reflection for memberProperties
                T::class.java.declaredFields.filter { !Modifier.isStatic(it.modifiers) }.forEach {
                    it.isAccessible = true
                    it.set(self, it.get(lazy))
                }
            }

            thisMethod.invoke(lazy, *args)

        } as T

        return proxy
    }
}

