package de.peekandpoke.ultra.slumber.builtin.polymorphism

import de.peekandpoke.ultra.slumber.AdditionalSerialName
import de.peekandpoke.ultra.slumber.Polymorphic
import de.peekandpoke.ultra.slumber.indexedSubClasses
import kotlinx.serialization.SerialName
import org.atteo.classindex.IndexSubclasses
import kotlin.reflect.KClass

sealed class PureSealedClass {

    data class A(val text: String) : PureSealedClass()

    data class B(val number: Int) : PureSealedClass()
}

sealed interface SealedInterface {

    @SerialName("A")
    data class A(val text: String) : SealedInterface

    @SerialName("B")
    data class B(val number: Int) : SealedInterface
}

sealed class CustomDiscriminator {

    companion object : Polymorphic.Parent {
        override val discriminator = "_"

        override val childTypes: Set<KClass<*>> get() = indexedSubClasses()
    }

    data class A(val text: String) : CustomDiscriminator() {
        companion object : Polymorphic.Child {
            override val identifier = "A"
        }
    }

    data class B(val number: Int) : CustomDiscriminator() {
        companion object : Polymorphic.Child {
            override val identifier = "B"
        }
    }
}

sealed class BaseWithDefaultType {

    companion object : Polymorphic.Parent {
        override val defaultType = A::class

        override val childTypes: Set<KClass<*>> get() = indexedSubClasses()
    }

    data class A(val text: String) : BaseWithDefaultType()

    data class B(val number: Int) : BaseWithDefaultType() {
        companion object : Polymorphic.Child {
            override val identifier = "Child_B"
        }
    }
}

sealed class AnnotatedChildrenBase {

    data class A(val text: String) : AnnotatedChildrenBase() {
        companion object : Polymorphic.TypedChild<A> {
            override val identifier = "Child_A"
        }
    }

    data class B(val number: Int) : AnnotatedChildrenBase() {
        companion object : Polymorphic.TypedChild<B> {
            override val identifier = "Child_B"
        }
    }
}

open class AnnotatedBase {

    companion object : Polymorphic.Parent {
        override val childTypes = setOf(A::class, B::class)
    }

    data class A(val text: String) : AnnotatedBase()

    data class B(val number: Int) : AnnotatedBase() {
        companion object : Polymorphic.Child {
            override val identifier = "Child_B"
        }
    }
}

sealed class SealedRoot {

    sealed class NestedA : SealedRoot() {

        data class DeeperA(val text: String) : NestedA()

        data class DeeperB(val text: String) : NestedA()
    }

    data class NestedB(val text: String) : SealedRoot()
}

@IndexSubclasses
open class ParentWithKlassIndex {

    companion object : Polymorphic.Parent {
        override val childTypes: Set<KClass<*>> get() = indexedSubClasses()
    }

    sealed class Sub1 : ParentWithKlassIndex() {
        data class Deeper1(val text: String) : Sub1()
        data class Deeper2(val text: String) : Sub1()
    }

    data class Sub2(val text: String) : ParentWithKlassIndex()
}

@IndexSubclasses
open class ChildrenUsingAnnotation {

    companion object : Polymorphic.Parent {
        override val childTypes: Set<KClass<*>> get() = indexedSubClasses()
    }

    @SerialName("Sub")
    sealed class Sub : ChildrenUsingAnnotation() {
        @SerialName("Sub.Deeper1")
        data class Deeper1(val text: String) : Sub()

        @SerialName("Sub.Deeper2")
        @AdditionalSerialName("Sub.Deeper2.Additional")
        data class Deeper2(val text: String) : Sub()
    }

    @SerialName("Sub2")
    data class Sub2(val text: String) : ChildrenUsingAnnotation()

    @SerialName("Sub3")
    @AdditionalSerialName("Sub3-Additional")
    @AdditionalSerialName("Sub3-Additional-2")
    data class Sub3(val num: Int) : ChildrenUsingAnnotation()
}
