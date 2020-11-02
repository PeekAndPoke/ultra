package de.peekandpoke.ultra.mutator.unit

import de.peekandpoke.ultra.mutator.SetMutator
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.DisplayName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  MutableSet<M> implementation
    ////

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

    "contains(M): must not trigger mutation" {

        val source = setOf(
            SomeDataClass("a", 1),
            SomeDataClass("b", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.contains(SomeDataClass("x", 10).mutator()) shouldBe false

            subject.contains(SomeDataClass("a", 1).mutator()) shouldBe true

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "containsAll(M): must not trigger mutation" {

        val source = setOf(
            SomeDataClass("a", 1),
            SomeDataClass("b", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.containsAll(listOf<SomeDataClassMutator>()) shouldBe true

            subject.containsAll(
                listOf(
                    SomeDataClass("a", 1).mutator()
                )
            ) shouldBe true

            subject.containsAll(
                listOf(
                    SomeDataClass("a", 1).mutator(),
                    SomeDataClass("x", 10).mutator()
                )
            ) shouldBe false

            subject.containsAll(
                listOf(
                    SomeDataClass("a", 1).mutator(),
                    SomeDataClass("b", 2).mutator()
                )
            ) shouldBe true

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "clear: clearing an empty set must not trigger mutation" {

        val source = setOf<SomeDataClass>()

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

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

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.clear()

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe setOf()
        }
    }

    "add(M): adding a mutator-element that is not present in the set must trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.add(
            SomeDataClass("third", 3).mutator()
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

    "add(M): adding a mutator-element that is already present in the set must NOT trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.add(
            SomeDataClass("second", 2).mutator()
        )

        assertSoftly {

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0

            subject.getResult() shouldBe setOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "addAll(M): adding an empty list or mutator-elements must NOT trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.addAll(listOf<SomeDataClassMutator>())

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "addAll(M): adding a list or mutator-elements where all are already present in the set must NOT trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2),
            SomeDataClass("third", 3)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.addAll(
                listOf(
                    SomeDataClass("first", 1).mutator(),
                    SomeDataClass("second", 2).mutator()
                )
            )

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "addAll(M): adding a list of mutator-elements must trigger mutation when at least one element is added" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.addAll(
                listOf(
                    SomeDataClass("first", 1).mutator(),
                    SomeDataClass("third", 3).mutator()
                )
            )

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe setOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2),
                SomeDataClass("third", 3)
            )
        }
    }

    "remove(M): removing a mutator-element that is not in the set must not trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.remove(
            SomeDataClass("third", 3).mutator()
        )

        assertSoftly {

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0

            subject.getResult() shouldBe setOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "remove(M): removing a mutator-element that is in the set must trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.remove(
            SomeDataClass("first", 1).mutator()
        )

        assertSoftly {

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe setOf(
                SomeDataClass("second", 2)
            )
        }
    }

    "removeAll(M): removing multiple mutator-elements that are not in the set must not trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.removeAll(listOf<SomeDataClassMutator>())

        subject.removeAll(
            listOf(
                SomeDataClass("third", 3).mutator()
            )
        )

        subject.removeAll(
            listOf(
                SomeDataClass("third", 3).mutator(),
                SomeDataClass("fourth", 4).mutator()
            )
        )

        assertSoftly {

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0

            subject.getResult() shouldBe setOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "removeAll(M): removing multiple mutator-elements that are in the set must trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.removeAll(
            listOf(
                SomeDataClass("first", 1).mutator(),
                SomeDataClass("third", 3).mutator()
            )
        )

        assertSoftly {

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe setOf(
                SomeDataClass("second", 2)
            )
        }
    }

    "retainAll(M): retaining exactly all mutator-elements that are in the set must not trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.retainAll(
            listOf(
                SomeDataClass("first", 1).mutator(),
                SomeDataClass("second", 2).mutator()
            )
        )

        assertSoftly {

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0

            subject.getResult() shouldBe setOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "retainAll(M): retaining some mutator-elements that are in the set must trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.retainAll(
            listOf(
                SomeDataClass("first", 1).mutator(),
                SomeDataClass("third", 3).mutator()
            )
        )

        assertSoftly {

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe setOf(
                SomeDataClass("first", 1)
            )
        }
    }

    "retainAll(M): retaining none of the mutator-elements that are in the set must trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.retainAll(
            listOf(
                SomeDataClass("third", 3).mutator(),
                SomeDataClass("fourth", 4).mutator()
            )
        )

        assertSoftly {

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe setOf()
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  additional implementation
    ////

    "contains(T): must not trigger mutation" {

        val source = setOf(
            SomeDataClass("a", 1),
            SomeDataClass("b", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.contains(SomeDataClass("x", 10)) shouldBe false

            subject.contains(SomeDataClass("a", 1)) shouldBe true

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "containsAll(T): must not trigger mutation" {

        val source = setOf(
            SomeDataClass("a", 1),
            SomeDataClass("b", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.containsAll(listOf<SomeDataClassMutator>()) shouldBe true

            subject.containsAll(
                listOf(
                    SomeDataClass("a", 1)
                )
            ) shouldBe true

            subject.containsAll(
                listOf(
                    SomeDataClass("a", 1),
                    SomeDataClass("x", 10)
                )
            ) shouldBe false

            subject.containsAll(
                listOf(
                    SomeDataClass("a", 1),
                    SomeDataClass("b", 2)
                )
            ) shouldBe true

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "add(T): adding an element must trigger mutation" {

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

    "addAll(T): adding an empty list of elements must NOT trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.addAll(listOf<SomeDataClass>())

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "addAll(T): adding a list of elements where all are already present in the set must NOT trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2),
            SomeDataClass("third", 3)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.addAll(
                listOf(
                    SomeDataClass("first", 1),
                    SomeDataClass("second", 2)
                )
            )

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "addAll(T): adding a list of elements must trigger mutation when at least one element is added" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.addAll(
                listOf(
                    SomeDataClass("first", 1),
                    SomeDataClass("third", 3)
                )
            )

            source shouldNotBeSameInstanceAs subject.getResult()

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

    "removeAll(T): removing multiple elements that are not in the set must not trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.removeAll(listOf<SomeDataClass>())

        subject.removeAll(
            listOf(
                SomeDataClass("third", 3).mutator()
            )
        )

        subject.removeAll(
            listOf(
                SomeDataClass("third", 3),
                SomeDataClass("fourth", 4)
            )
        )

        assertSoftly {

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0

            subject.getResult() shouldBe setOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "removeAll(T): removing multiple elements that are in the set must trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.removeAll(
            listOf(
                SomeDataClass("first", 1),
                SomeDataClass("third", 3)
            )
        )

        assertSoftly {

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe setOf(
                SomeDataClass("second", 2)
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

    "retain(T): retaining exactly all elements that are in the set must not trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.retain(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        assertSoftly {

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0

            subject.getResult() shouldBe setOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "retain(T): retaining some elements that are in the set must trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.retain(
            SomeDataClass("first", 1),
            SomeDataClass("third", 3)
        )

        assertSoftly {

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe setOf(
                SomeDataClass("first", 1)
            )
        }
    }

    "retainAll(Collection<T>): retaining none of the elements that are in the set must trigger mutation" {

        val source = setOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = SetMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.retainAll(
            listOf(
                SomeDataClass("third", 3),
                SomeDataClass("fourth", 4)
            )
        )

        assertSoftly {

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe setOf()
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
})
