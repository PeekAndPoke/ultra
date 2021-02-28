package de.peekandpoke.ultra.common

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class ListsSpec : StringSpec({

    val old = Pair(1, 2)
    val old2 = Pair(1, 2)
    val new = Pair(3, 4)
    val other = Pair(5, 6)

    //  List.replace  //////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row(listOf(), "old", "new", listOf()),
        row(listOf("old"), "old", "new", listOf("new")),
        row(listOf("old", "old"), "old", "new", listOf("new", "new")),
        row(listOf("x", "old", "new", "old", "x"), "old", "new", listOf("x", "new", "new", "new", "x")),
        // here we check the none strict equality between 'old' and 'old2'
        row(listOf(old), old, new, listOf(new)),
        row(listOf(old2), old, new, listOf(new)),
        row(listOf(old, old2), old, new, listOf(new, new)),
        // more complex example
        row(listOf(other, old, new, old, other), old, new, listOf(other, new, new, new, other))
    ).forEachIndexed { testIdx, (source, old, new, expected) ->

        "List.replace #$testIdx: '$source', old: '$old', new: '$new' should be '$expected'" {
            source.replace(old, new) shouldBe expected
        }
    }

    //  List.replaceStrict  ////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row(listOf(), "old", "new", listOf()),
        row(listOf("old"), "old", "new", listOf("new")),
        row(listOf("old", "old"), "old", "new", listOf("new", "new")),
        row(listOf("x", "old", "new", "old", "x"), "old", "new", listOf("x", "new", "new", "new", "x")),
        // here we check the none strict equality between 'old' and 'old2'
        row(listOf(old), old, new, listOf(new)),
        row(listOf(old2), old, new, listOf(old2)),
        row(listOf(old, old2), old, new, listOf(new, old2)),
        // more complex example
        row(listOf(other, old, new, old, other), old, new, listOf(other, new, new, new, other))
    ).forEachIndexed { testIdx, (source, old, new, expected) ->

        "List.replaceStrict #$testIdx: '$source', old: '$old', new: '$new' should be '$expected'" {
            source.replaceStrict(old, new) shouldBe expected
        }
    }

    //  List.replaceAt  ////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row(listOf("a"), 0, "z", listOf("z")),
        row(listOf("a", "b"), 1, "z", listOf("a", "z"))
    ).forEachIndexed { testIdx, (source, idx, new, expected) ->

        "List.replaceAt #$testIdx: '$source', idx: '$idx', new: '$new' should be '$expected'" {
            source.replaceAt(idx, new) shouldBe expected
        }
    }

    "List.replaceAt with idx -1 should throw" {
        shouldThrow<IndexOutOfBoundsException> {
            listOf("a").replaceAt(-1, "z")
        }
    }

    "List.replaceAt with idx >= size should throw" {
        shouldThrow<IndexOutOfBoundsException> {
            listOf("a").replaceAt(1, "z")
        }
    }

    //  List.remove  ///////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row(listOf(), old, listOf()),
        row(listOf(old), old, listOf()),
        row(listOf(old2), old, listOf()),
        row(listOf(old, other), old, listOf(other)),
        // here we check the none strict comparison between 'old' and 'old2'
        row(listOf(old2), old, listOf()),
        row(listOf(old, new, old2, other), old, listOf(new, other))
    ).forEachIndexed { testIdx, (source, element, expected) ->

        "List.remove $testIdx: '$source', element: '$element' shouldBe '$expected'" {
            source.remove(element) shouldBe expected
        }
    }

    //  List.removeStrict  /////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row(listOf(), old, listOf()),
        row(listOf(old), old, listOf()),
        row(listOf(old2), old, listOf(old2)),
        row(listOf(old, other), old, listOf(other)),
        row(listOf(old, new, old2, other), old, listOf(new, old2, other))
    ).forEachIndexed { testIdx, (source, element, expected) ->

        "List.removeStrict #$testIdx: '$source', element: '$element' shouldBe '$expected'" {
            source.removeStrict(element) shouldBe expected
        }
    }

    //  List.removeAt  /////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row(listOf("a"), 0, listOf()),
        row(listOf("a", "b"), 0, listOf("b")),
        row(listOf("a", "b"), 1, listOf("a"))
    ).forEachIndexed { testIdx, (source, idx, expected) ->

        "List.removeAt #$testIdx: '$source', idx: '$idx' should be '$expected'" {
            source.removeAt(idx) shouldBe expected
        }
    }

    "List.removeAt with idx -1 should throw" {
        shouldThrow<IndexOutOfBoundsException> {
            listOf("a").removeAt(-1)
        }
    }

    "List.removeAt with idx >= size should throw" {
        shouldThrow<IndexOutOfBoundsException> {
            listOf("a").removeAt(1)
        }
    }

    //  List.addAt  ////////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row(listOf(), 0, "z", listOf("z")),
        row(listOf("a"), 0, "z", listOf("z", "a")),
        row(listOf("a"), 1, "z", listOf("a", "z"))
    ).forEachIndexed { testIdx, (source, idx, new, expected) ->

        "List.addAt #$testIdx: '$source', idx: '$idx', new: '$new' should be '$expected'" {
            source.addAt(idx, new) shouldBe expected
        }
    }

    "List.addAt with idx -1 should throw" {
        shouldThrow<IndexOutOfBoundsException> {
            listOf("a").addAt(-1, "z")
        }
    }

    "List.addAt with idx > size should throw" {
        shouldThrow<IndexOutOfBoundsException> {
            listOf("a").addAt(2, "z")
        }
    }

    //  List.addAt  ////////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row(listOf(), 0, 1, listOf()),
        row(listOf("a"), 0, 1, listOf("a")),
        row(listOf("a", "b"), 0, 1, listOf("b", "a")),
        row(listOf("a", "b", "c"), 0, 1, listOf("b", "a", "c")),
        row(listOf("a", "b", "c"), 2, 1, listOf("a", "c", "b"))
    ).forEachIndexed { testIdx, (source, idx1, idx2, expected) ->

        "List.swapAt #$testIdx: '$source', idx1: '$idx1', idx2: '$idx2' should be '$expected'" {
            source.swapAt(idx1, idx2) shouldBe expected
        }
    }
})
