package de.peekandpoke.ultra.mutator.unit

import de.peekandpoke.ultra.mutator.ListMutator
import io.kotlintest.DisplayName
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  MutableList<M> implementation
    ////

    "iterator: getting the iterator must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })


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

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

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

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

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

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.isEmpty() shouldBe false

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "contains(M): must not trigger mutation" {

        val source = listOf(
            SomeDataClass("a", 1),
            SomeDataClass("b", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.contains(SomeDataClass("x", 10).mutator()) shouldBe false

            subject.contains(SomeDataClass("a", 1).mutator()) shouldBe true

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "containsAll(M): must not trigger mutation" {

        val source = listOf(
            SomeDataClass("a", 1),
            SomeDataClass("b", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

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

    "indexOf(M): must not trigger mutation" {

        val source = listOf(
            SomeDataClass("a", 1),
            SomeDataClass("b", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.indexOf(SomeDataClass("x", 10).mutator()) shouldBe -1

            subject.indexOf(SomeDataClass("a", 1).mutator()) shouldBe 0

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "lastIndexOf(M): must not trigger mutation" {

        val source = listOf(
            SomeDataClass("a", 1),
            SomeDataClass("b", 2),
            SomeDataClass("a", 1)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.lastIndexOf(SomeDataClass("x", 10).mutator()) shouldBe -1

            subject.lastIndexOf(SomeDataClass("a", 1).mutator()) shouldBe 2

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "clear: clearing an empty list must not trigger mutation" {

        val source = listOf<SomeDataClass>()

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

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

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.clear()

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe listOf()
        }
    }

    "get: getting an element from the list must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject[0].getResult() shouldBe SomeDataClass("first", 1)

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "set(M): trying to set a mutator-element out of bounds must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            shouldThrow<IndexOutOfBoundsException> {
                apply {
                    subject[100] = SomeDataClass("third", 3).mutator()
                }
            }

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "set(M): setting the same mutator-value on an element must not trigger mutation" {

        val first = SomeDataClass("first", 1)

        val source = listOf(
            first,
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject[0] = first.mutator()

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "set(M): setting a different mutator-value on an element must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject[0] = SomeDataClass("third", 3).mutator()

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("third", 3),
                SomeDataClass("second", 2)
            )
        }
    }

    "add(M): adding a mutator-element at the end must work" {
        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.add(SomeDataClass("third", 3).mutator())

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2),
                SomeDataClass("third", 3)
            )
        }
    }

    "add(M): adding a mutator-element at an index must work" {
        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.add(1, SomeDataClass("third", 3).mutator())

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1),
                SomeDataClass("third", 3),
                SomeDataClass("second", 2)
            )
        }
    }

    "addAll(M): adding an empty list or mutator-elements at the end must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.addAll(listOf<SomeDataClassMutator>())

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "addAll(M): adding a non-empty list or mutator-elements at the end" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.addAll(
                listOf(
                    SomeDataClass("third", 3).mutator()
                )
            )

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2),
                SomeDataClass("third", 3)
            )
        }
    }

    "addAll(M): adding an empty list or mutator-elements at an index must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.addAll(0, listOf<SomeDataClassMutator>())

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "addAll(M): adding a non-empty list or mutator-elements at an index" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.addAll(
                0,
                listOf(
                    SomeDataClass("third", 3).mutator()
                )
            )

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("third", 3),
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "remove(M): removing a mutator-element that is not in the list must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.remove(
            SomeDataClass("third", 3).mutator()
        )

        assertSoftly {

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "remove(M): removing a mutator-element that is in the list must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.remove(
            SomeDataClass("first", 1).mutator()
        )

        assertSoftly {

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("second", 2)
            )
        }
    }

    "removeAt: trying to remove by an index that is out of bounds must throw an exception" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {
            shouldThrow<IndexOutOfBoundsException> {
                subject.removeAt(-1)
            }
            shouldThrow<IndexOutOfBoundsException> {
                subject.removeAt(2)
            }
        }
    }

    "removeAt: removing by an index that is within the bounds must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.removeAt(1)

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1)
            )
        }
    }

    "removeAll(M): removing multiple mutator-elements that are not in the list must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

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

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "removeAll(M): removing multiple mutator-elements that are in the list must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.removeAll(
            listOf(
                SomeDataClass("first", 1).mutator(),
                SomeDataClass("third", 3).mutator()
            )
        )

        assertSoftly {

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("second", 2)
            )
        }
    }

    "retainAll(M): retaining exactly all mutator-elements that are in the list must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.retainAll(
            listOf(
                SomeDataClass("first", 1).mutator(),
                SomeDataClass("second", 2).mutator()
            )
        )

        assertSoftly {

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "retainAll(M): retaining some mutator-elements that are in the list must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.retainAll(
            listOf(
                SomeDataClass("first", 1).mutator(),
                SomeDataClass("third", 3).mutator()
            )
        )

        assertSoftly {

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1)
            )
        }
    }

    "retainAll(M): retaining none of the mutator-elements that are in the list must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.retainAll(
            listOf(
                SomeDataClass("third", 3).mutator(),
                SomeDataClass("fourth", 4).mutator()
            )
        )

        assertSoftly {

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe listOf()
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  additional functionality implementation
    ////

    "contains(T): must not trigger mutation" {

        val source = listOf(
            SomeDataClass("a", 1),
            SomeDataClass("b", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.contains(SomeDataClass("x", 10)) shouldBe false

            subject.contains(SomeDataClass("a", 1)) shouldBe true

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "containsAll(T): must not trigger mutation" {

        val source = listOf(
            SomeDataClass("a", 1),
            SomeDataClass("b", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.containsAll(listOf<SomeDataClass>()) shouldBe true

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

    "indexOf(T): must not trigger mutation" {

        val source = listOf(
            SomeDataClass("a", 1),
            SomeDataClass("b", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.indexOf(SomeDataClass("x", 10)) shouldBe -1

            subject.indexOf(SomeDataClass("a", 1)) shouldBe 0

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "lastIndexOf(T): must not trigger mutation" {

        val source = listOf(
            SomeDataClass("a", 1),
            SomeDataClass("b", 2),
            SomeDataClass("a", 1)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.lastIndexOf(SomeDataClass("x", 10)) shouldBe -1

            subject.lastIndexOf(SomeDataClass("a", 1)) shouldBe 2

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "add(T): adding a element at the end must work" {
        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.add(SomeDataClass("third", 3))

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2),
                SomeDataClass("third", 3)
            )
        }
    }

    "add(T): adding an element at an index must work" {
        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.add(1, SomeDataClass("third", 3))

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1),
                SomeDataClass("third", 3),
                SomeDataClass("second", 2)
            )
        }
    }

    "addAll(T): adding an empty list of elements at the end must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.addAll(listOf<SomeDataClass>())

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "addAll(T): adding a non-empty list or elements at the end" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.addAll(
                listOf(
                    SomeDataClass("third", 3)
                )
            )

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2),
                SomeDataClass("third", 3)
            )
        }
    }

    "addAll(T): adding an empty list of elements at an index must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.addAll(0, listOf<SomeDataClass>())

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0
        }
    }

    "addAll(T): adding a non-empty list of elements at an index" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        assertSoftly {

            subject.addAll(
                0,
                listOf(
                    SomeDataClass("third", 3)
                )
            )

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("third", 3),
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "push(T): pushing an element must trigger mutation" {

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

    "pop: T: popping from an empty list must not trigger mutation" {

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

    "pop: T: popping from a non-empty list must trigger mutation" {

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

    "unshift(T): unshifting an element must trigger mutation" {

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

    "shift: T: shifting from an empty list must not trigger mutation" {

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

    "shift: T: shifting from a non-empty list must trigger mutation" {

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

    "remove(T): removing an element that is not in the list must not trigger mutation" {

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

    "remove(T): removing an element that is in the list must trigger mutation" {

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

    "removeAll(Collection<T>): removing multiple elements that are in the list must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        subject.removeAll(
            listOf(
                SomeDataClass("first", 1),
                SomeDataClass("third", 3)
            )
        )

        assertSoftly {

            (source === subject.getResult()) shouldBe false

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("second", 2)
            )
        }
    }

    "retain(T): retaining exactly all elements that are in the list must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.retain(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        assertSoftly {

            source shouldBeSameInstanceAs subject.getResult()

            modifications shouldBe 0

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1),
                SomeDataClass("second", 2)
            )
        }
    }

    "retain(T): retaining some elements that are in the list must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.retain(
            SomeDataClass("first", 1),
            SomeDataClass("third", 3)
        )

        assertSoftly {

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe listOf(
                SomeDataClass("first", 1)
            )
        }
    }

    "retainAll(Collection<T>): retaining none of the elements that are in the list must trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> it.mutator(mod) }, { it.getResult() })

        subject.retainAll(
            listOf(
                SomeDataClass("third", 3),
                SomeDataClass("fourth", 4)
            )
        )

        assertSoftly {

            source shouldNotBeSameInstanceAs subject.getResult()

            modifications shouldBe 1

            subject.getResult() shouldBe listOf()
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

    "set(T): trying to set an element out of bounds must not trigger mutation" {

        val source = listOf(
            SomeDataClass("first", 1),
            SomeDataClass("second", 2)
        )

        var modifications = 0

        val subject = ListMutator(source, { modifications++ }, { it, mod -> Wrapper(it, mod) }, { it.value })

        assertSoftly {

            shouldThrow<IndexOutOfBoundsException> {
                apply {
                    subject[100] = SomeDataClass("third", 3)
                }
            }

            (source === subject.getResult()) shouldBe true

            modifications shouldBe 0
        }
    }

    "set(T): setting the same value on an element must not trigger mutation" {

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

    "set(T): setting a different value on an element must trigger mutation" {

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
