package io.peekandpoke.ultra.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.common.recursion.flattenTreeToSet
import io.peekandpoke.ultra.common.recursion.recurse

private class Node(val name: String, var parent: Node? = null)

private data class TreeNode(val name: String, val children: List<TreeNode> = emptyList())

class RecursionSpec : StringSpec({

    "recurse follows parent chain" {
        val root = Node("root")
        val child = Node("child", parent = root)
        val grandchild = Node("grandchild", parent = child)

        val chain = grandchild.recurse { parent }

        chain.map { it.name } shouldBe listOf("grandchild", "child", "root")
    }

    "recurse stops at null parent" {
        val root = Node("root")
        val chain = root.recurse { parent }

        chain.map { it.name } shouldBe listOf("root")
    }

    "recurse detects cycles" {
        val a = Node("a")
        val b = Node("b")
        // Create cycle: a → b → a
        a.parent = b
        b.parent = a

        val chain = a.recurse { parent }

        // Should stop when it encounters 'a' again (already in result)
        chain.map { it.name } shouldBe listOf("a", "b")
    }

    "recurse detects self-reference" {
        val a = Node("a")
        a.parent = a

        val chain = a.recurse { parent }

        chain.map { it.name } shouldBe listOf("a")
    }

    "flattenTreeToSet flattens a tree" {
        val tree = TreeNode(
            "root", listOf(
                TreeNode("a", listOf(TreeNode("a1"), TreeNode("a2"))),
                TreeNode("b", listOf(TreeNode("b1"))),
            )
        )

        val flat = tree.flattenTreeToSet { it.children }

        flat.map { it.name }.toSet() shouldBe setOf("root", "a", "a1", "a2", "b", "b1")
    }

    "flattenTreeToSet handles leaf node" {
        val leaf = TreeNode("leaf")

        val flat = leaf.flattenTreeToSet { it.children }

        flat.map { it.name } shouldBe listOf("leaf")
    }

    "flattenTreeToSet handles duplicate children" {
        val shared = TreeNode("shared")
        val tree = TreeNode(
            "root", listOf(
                TreeNode("a", listOf(shared)),
                TreeNode("b", listOf(shared)),
            )
        )

        val flat = tree.flattenTreeToSet { it.children }

        // "shared" appears only once
        flat.count { it.name == "shared" } shouldBe 1
    }
})
