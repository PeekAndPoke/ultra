package de.peekandpoke.ultra.slumber.builtin.polymorphism

import com.github.matfax.klassindex.IndexSubclasses

sealed class PureBase {

    data class A(val text: String) : PureBase()

    data class B(val number: Int) : PureBase()
}

sealed class CustomDiscriminator {

    companion object : Polymorphic.Parent {
        override val discriminator = "_"
    }

    data class A(val text: String) : CustomDiscriminator()

    data class B(val number: Int) : CustomDiscriminator()
}

sealed class BaseWithDefaultType {

    companion object : Polymorphic.Parent {
        override val defaultType = A::class
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

sealed class NestedRoot {

    sealed class NestedA : NestedRoot() {

        data class DeeperA(val text: String) : NestedA()

        data class DeeperB(val text: String) : NestedA()
    }

    data class NestedB(val text: String) : NestedRoot()
}

@IndexSubclasses
open class ParentWithKlassIndex {

    companion object : Polymorphic.Parent {
//        override val childTypes: Set<KClass<*>>
//            get() = ParentWithKlassIndex::class.indexedSubClasses
    }

    sealed class Sub1 : ParentWithKlassIndex() {
        data class Deeper1(val text: String) : Sub1()
        data class Deeper2(val text: String) : Sub1()
    }

    data class Sub2(val text: String) : ParentWithKlassIndex()
}
