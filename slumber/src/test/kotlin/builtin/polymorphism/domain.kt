package de.peekandpoke.ultra.slumber.builtin.polymorphism

import de.peekandpoke.ultra.slumber.Polymorphic
import de.peekandpoke.ultra.slumber.indexedSubClasses
import kotlinx.serialization.SerialName
import org.atteo.classindex.IndexSubclasses
import kotlin.reflect.KClass

sealed class PureBase {

    data class A(val text: String) : PureBase()

    data class B(val number: Int) : PureBase()
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

sealed class AnnotedChildrenBase {

    data class A(val text: String) : AnnotedChildrenBase() {
        companion object : Polymorphic.Child {
            override val identifier = "Child_A"
        }
    }

    data class B(val number: Int) : AnnotedChildrenBase() {
        companion object : Polymorphic.Child {
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
open class ParentWithClassIndex {

    companion object : Polymorphic.Parent {
        override val childTypes: Set<KClass<*>> get() = indexedSubClasses()
    }

    sealed class Sub1 : ParentWithClassIndex() {
        data class Deeper1(val text: String) : Sub1()
        data class Deeper2(val text: String) : Sub1()
    }

    data class Sub2(val text: String) : ParentWithClassIndex()
}

@IndexSubclasses
open class ParentWithChildrenUsingAnnotation {

    companion object : Polymorphic.Parent {
        override val childTypes: Set<KClass<*>> get() = indexedSubClasses()
    }

    @SerialName("Sub")
    sealed class Sub : ParentWithChildrenUsingAnnotation() {
        @SerialName("Sub.Deeper1")
        data class Deeper1(val text: String) : Sub()

        @SerialName("Sub.Deeper2")
        data class Deeper2(val text: String) : Sub()
    }

    @SerialName("Sub2")
    data class Sub2(val text: String) : ParentWithChildrenUsingAnnotation()
}
