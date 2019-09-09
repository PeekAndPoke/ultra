package de.peekandpoke.ultra.mutator.unit

import de.peekandpoke.ultra.mutator.ListMutator
import io.kotlintest.DisplayName
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

@DisplayName("UNIT - ListMutatorSpec")
class ListMutatorSpec : StringSpec({

    "onModify triggered on a list element must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject[0].onModify(SomeDataClass("first", 1))

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1
        }
    }

    "plusAssign: assigning the same list must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject += source

        assertSoftly {

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "plusAssign: assigning a different list must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject += source.toList()

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1
        }
    }

    "iterator: getting the iterator must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })


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

    "size: must not trigger mutation on empty list" {

        val source = listOf<SomeDataClass>()

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject.size shouldBe 0

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "size: must not trigger mutation on non-empty list" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject.size shouldBe 2

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "isEmpty: must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject.isEmpty() shouldBe false

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "clear: clearing an empty list must not trigger mutation" {

        val source = listOf<SomeDataClass>()

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.clear()

        assertSoftly {

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "clear: clearing an non empty list must work and trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.clear()

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe listOf()
        }
    }

    "push: pushing an element must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.push(
            SomeDataClass("third", 3)
        )

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2),
                SomeDataClass("third", 3)
            )
        }
    }

    "pop: popping from an empty list must not trigger mutation" {

        val source = listOf<SomeDataClass>()

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject.pop() shouldBe null

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0

            subject.getResult() shouldBe listOf()
        }
    }

    "pop: popping from a non-empty list must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject.pop() shouldBe SomeDataClass("second", 2)

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1)
            )
        }
    }

    "unshift: unshifting an element must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.unshift(
            SomeDataClass("third", 3)
        )

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("third", 3),
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "shift: shifting from an empty list must not trigger mutation" {

        val source = listOf<SomeDataClass>()

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject.shift() shouldBe null

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0

            subject.getResult() shouldBe listOf()
        }
    }

    "shift: shifting from a non-empty list must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject.shift() shouldBe SomeDataClass("first", 1)

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("second", 2)
            )
        }
    }

    "remove: removing an element that is not in the list must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.remove(
            SomeDataClass("third", 3)
        )

        assertSoftly {

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "remove: removing an element that is in the list must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.remove(
            SomeDataClass("first", 1)
        )

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("second", 2)
            )
        }
    }

    "removeAt: trying to remove by an index that is out of bounds must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.removeAt(-1)
        subject.removeAt(2)

        assertSoftly {

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "removeAt: removing by an index that is within the bounds must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.removeAt(1)

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1)
            )
        }
    }

    "retainWhere: retaining all elements must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.retainWhere { true }

        assertSoftly {

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "retainWhere: retaining not all elements must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.retainWhere { aString == "first" }

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1)
            )
        }
    }

    "removeWhere: removing no elements must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.removeWhere { false }

        assertSoftly {

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "removeWhere: removing some elements must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.removeWhere { aString != "first" }

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1)
            )
        }
    }

    "get: getting an element from the list must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject[0].value shouldBe SomeDataClass("first", 1)

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "set: trying to set an element out of bounds must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            try {
                subject[100] = SomeDataClass("third", 3)
            } catch (e: Throwable) {
            }

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "set: setting the same value on an element must not trigger mutation" {

        val first = SomeDataClass("first", 1)

        val source = listOf(
            first,
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject[0] = first

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "set: setting a different value on an element must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            subject[0] = SomeDataClass("third", 3)

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("third", 3),
                SomeDataClass("second", 2)
            )
        }
    }
})
