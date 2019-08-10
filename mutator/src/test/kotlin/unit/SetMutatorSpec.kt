package de.peekandpoke.ultra.mutator.unit

import de.peekandpoke.ultra.mutator.SetMutator
import io.kotlintest.DisplayName
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

@DisplayName("UNIT - SetMutatorSpec")
class SetMutatorSpec : StringSpec({

    "onModify triggered on a set element must trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject.first().onModify(SomeDataClass("first", 1))

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1
        }
    }

    "plusAssign: assigning the same set must not trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject += source

        assertSoftly {

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "plusAssign: assigning a different set must trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject += source.toSet()

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1
        }
    }

    "iterator: getting the iterator must not trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject.iterator()

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0

            @Suppress("ForEachParameterNotUsed")
            subject.forEach { }

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "size: must not trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject.size shouldBe 2

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "isEmpty: must not trigger mutation on empty set" {

        val source = setOf<SomeDataClass>()

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject.isEmpty() shouldBe true

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "isEmpty: must not trigger mutation on non-empty set" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject.isEmpty() shouldBe false

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "clear: clearing an empty set must not trigger mutation" {

        val source = setOf<SomeDataClass>()

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.clear()

        assertSoftly {

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "clear: clearing an non empty set must work and trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.clear()

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe setOf()
        }
    }

    "add: adding an element must trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.add(
            SomeDataClass("third", 3)
        )

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe setOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2),
                SomeDataClass("third", 3)
            )
        }
    }

    "remove: removing an element that is not in the set must not trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.remove(
            SomeDataClass("third", 3)
        )

        assertSoftly {

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0

            subject.getResult() shouldBe setOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "remove: removing an element that is in the set must trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.remove(
            SomeDataClass("first", 1)
        )

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe setOf(
                SomeDataClass("second", 2)
            )
        }
    }

    "retainWhere: retaining all elements must not trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.retainWhere { true }

        assertSoftly {

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0

            subject.getResult() shouldBe setOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "retainWhere: retaining not all elements must trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.retainWhere { aString == "first" }

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe setOf(
                SomeDataClass("first", 1)
            )
        }
    }

    "removeWhere: removing no elements must not trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.removeWhere { false }

        assertSoftly {

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0

            subject.getResult() shouldBe setOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "removeWhere: removing some elements must trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.removeWhere { aString != "first" }

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe setOf(
                SomeDataClass("first", 1)
            )
        }
    }
})
