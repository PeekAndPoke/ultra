package de.peekandpoke.ultra.common

import de.peekandpoke.ultra.common.model.tuple
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ListsSpec : StringSpec({

    val old = Pair(1, 2)
    val old2 = Pair(1, 2)
    val new = Pair(3, 4)
    val other = Pair(5, 6)

    //  List.replace  //////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple(listOf(), "old", "new", listOf()),
        tuple(listOf("old"), "old", "new", listOf("new")),
        tuple(listOf("old", "old"), "old", "new", listOf("new", "new")),
        tuple(listOf("x", "old", "new", "old", "x"), "old", "new", listOf("x", "new", "new", "new", "x")),
        // here we check the none strict equality between 'old' and 'old2'
        tuple(listOf(old), old, new, listOf(new)),
        tuple(listOf(old2), old, new, listOf(new)),
        tuple(listOf(old, old2), old, new, listOf(new, new)),
        // more complex example
        tuple(listOf(other, old, new, old, other), old, new, listOf(other, new, new, new, other))
    ).forEachIndexed { testIdx, (source, old, new, expected) ->

        "List.replace #$testIdx: '$source', old: '$old', new: '$new' should be '$expected'" {
            source.replace(old, new) shouldBe expected
        }
    }

    //  List.replaceStrict  ////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple(listOf(), "old", "new", listOf()),
        tuple(listOf("old"), "old", "new", listOf("new")),
        tuple(listOf("old", "old"), "old", "new", listOf("new", "new")),
        tuple(listOf("x", "old", "new", "old", "x"), "old", "new", listOf("x", "new", "new", "new", "x")),
        // here we check the none strict equality between 'old' and 'old2'
        tuple(listOf(old), old, new, listOf(new)),
        tuple(listOf(old2), old, new, listOf(old2)),
        tuple(listOf(old, old2), old, new, listOf(new, old2)),
        // more complex example
        tuple(listOf(other, old, new, old, other), old, new, listOf(other, new, new, new, other))
    ).forEachIndexed { testIdx, (source, old, new, expected) ->

        "List.replaceStrict #$testIdx: '$source', old: '$old', new: '$new' should be '$expected'" {
            source.replaceStrict(old, new) shouldBe expected
        }
    }

    //  List.replaceAt  ////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple(listOf("a"), 0, "z", listOf("z")),
        tuple(listOf("a", "b"), 1, "z", listOf("a", "z"))
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
        tuple(listOf(), old, listOf()),
        tuple(listOf(old), old, listOf()),
        tuple(listOf(old2), old, listOf()),
        tuple(listOf(old, other), old, listOf(other)),
        // here we check the none strict comparison between 'old' and 'old2'
        tuple(listOf(old2), old, listOf()),
        tuple(listOf(old, new, old2, other), old, listOf(new, other))
    ).forEachIndexed { testIdx, (source, element, expected) ->

        "List.remove $testIdx: '$source', element: '$element' shouldBe '$expected'" {
            source.remove(element) shouldBe expected
        }
    }

    //  List.removeStrict  /////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple(listOf(), old, listOf()),
        tuple(listOf(old), old, listOf()),
        tuple(listOf(old2), old, listOf(old2)),
        tuple(listOf(old, other), old, listOf(other)),
        tuple(listOf(old, new, old2, other), old, listOf(new, old2, other))
    ).forEachIndexed { testIdx, (source, element, expected) ->

        "List.removeStrict #$testIdx: '$source', element: '$element' shouldBe '$expected'" {
            source.removeStrict(element) shouldBe expected
        }
    }

    //  List.removeAt  /////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple(listOf("a"), 0, listOf()),
        tuple(listOf("a", "b"), 0, listOf("b")),
        tuple(listOf("a", "b"), 1, listOf("a"))
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
        tuple(listOf(), 0, "z", listOf("z")),
        tuple(listOf("a"), 0, "z", listOf("z", "a")),
        tuple(listOf("a"), 1, "z", listOf("a", "z"))
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
        tuple(listOf(), 0, 1, listOf()),
        tuple(listOf("a"), 0, 1, listOf("a")),
        tuple(listOf("a", "b"), 0, 1, listOf("b", "a")),
        tuple(listOf("a", "b", "c"), 0, 1, listOf("b", "a", "c")),
        tuple(listOf("a", "b", "c"), 2, 1, listOf("a", "c", "b"))
    ).forEachIndexed { testIdx, (source, idx1, idx2, expected) ->

        "List.swapAt #$testIdx: '$source', idx1: '$idx1', idx2: '$idx2' should be '$expected'" {
            source.swapAt(idx1, idx2) shouldBe expected
        }
    }

    //  List.replaceFirstByOrAdd  //////////////////////////////////////////////////////////////////////////////////////

    "List.replaceFirstByOrAdd adding an item that is not yet in the list - data class" {

        data class Data(val id: String, val n: Int)

        val source: List<Data> = listOf(
            Data(id = "1", n = 1),
            Data(id = "2", n = 2),
            Data(id = "3", n = 3),
        )

        val result = source.replaceFirstByOrAdd(Data(id = "5", n = 100)) { it.n }

        result shouldBe listOf(
            Data(id = "1", n = 1),
            Data(id = "2", n = 2),
            Data(id = "3", n = 3),
            Data(id = "5", n = 100),
        )
    }

    "List.replaceFirstByOrAdd replacing an item in the list - data class" {

        data class Data(val id: String, val n: Int)

        val source: List<Data> = listOf(
            Data(id = "1", n = 1),
            Data(id = "2", n = 2),
            Data(id = "3", n = 3),
        )

        val result = source.replaceFirstByOrAdd(Data(id = "1", n = 100)) { it.id }

        result shouldBe listOf(
            Data(id = "1", n = 100),
            Data(id = "2", n = 2),
            Data(id = "3", n = 3),
        )
    }
})
