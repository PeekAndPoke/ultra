package de.peekandpoke.ultra.mutator.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class InterfaceInheritanceSpec : StringSpec({

    "Mutating child classes for a property only defined with an interface type" {

        val subject = Meadow(
            listOf(
                TreeBranchLeft(10.0f, 5.0f, 7.5f),
                TreeBranchRight(6.0f, 3.0f, 40.0f),
                TreeBranchRight(6.1f, 3.1f, 45.0f)
            )
        )

        val result = subject.mutate {
            trees.forEach { tree ->

                when (tree) {
                    is TreeBranchLeftMutator -> {
                        tree.rootDepth = 11.1f
                        tree.branchLength = 8.5f
                    }

                    is TreeBranchRightMutator -> {
                        tree.trunkWidth += 1.1f
                        tree.branchWeight = 46.0f
                    }
                }
            }
        }

        assertSoftly {
            result shouldBe Meadow(
                listOf(
                    TreeBranchLeft(11.1f, 5.0f, 8.5f),
                    TreeBranchRight(6.0f, 4.1f, 46.0f),
                    TreeBranchRight(6.1f, 4.2f, 46.0f)
                )
            )
        }
    }
})
