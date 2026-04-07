package io.peekandpoke.monko

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.reflection.kType

class MonkoIndexEnsureResultSpec : FreeSpec() {

    data class TestEntity(val name: String)

    private class TestRepo : MonkoRepository<TestEntity>(
        name = "test_entities",
        storedType = kType(),
        driver = createMinimalDriver(),
    )

    companion object {
        private fun createMinimalDriver(): MonkoDriver = MonkoDriver(
            lazyCodec = lazy { error("Not needed") },
            lazyClient = lazy { error("Not needed") },
            lazyDatabase = lazy { error("Not needed") },
        )
    }

    init {
        "MonkoIndexBuilder.EnsureResult" - {

            "Ensured" - {

                "should hold correct properties" {
                    val repo = TestRepo()
                    val result = MonkoIndexBuilder.EnsureResult.Ensured(
                        repo = repo,
                        name = "idx_name",
                        fields = listOf("name", "age"),
                    )

                    result.repo shouldBe repo
                    result.name shouldBe "idx_name"
                    result.fields shouldContainExactly listOf("name", "age")
                    result.shouldBeInstanceOf<MonkoIndexBuilder.EnsureResult.Ensured>()
                }
            }

            "Kept" - {

                "should hold correct properties" {
                    val repo = TestRepo()
                    val result = MonkoIndexBuilder.EnsureResult.Kept(
                        repo = repo,
                        name = "idx_existing",
                        fields = listOf("email"),
                    )

                    result.repo shouldBe repo
                    result.name shouldBe "idx_existing"
                    result.fields shouldContainExactly listOf("email")
                    result.shouldBeInstanceOf<MonkoIndexBuilder.EnsureResult.Kept>()
                }
            }

            "ReCreated" - {

                "should hold correct properties" {
                    val repo = TestRepo()
                    val result = MonkoIndexBuilder.EnsureResult.ReCreated(
                        repo = repo,
                        name = "idx_recreated",
                        fields = listOf("status"),
                    )

                    result.repo shouldBe repo
                    result.name shouldBe "idx_recreated"
                    result.fields shouldContainExactly listOf("status")
                    result.shouldBeInstanceOf<MonkoIndexBuilder.EnsureResult.ReCreated>()
                }
            }

            "Error" - {

                "should hold the exception" {
                    val repo = TestRepo()
                    val exception = RuntimeException("Index creation failed")
                    val result = MonkoIndexBuilder.EnsureResult.Error(
                        repo = repo,
                        name = "idx_failed",
                        fields = listOf("broken"),
                        error = exception,
                    )

                    result.repo shouldBe repo
                    result.name shouldBe "idx_failed"
                    result.fields shouldContainExactly listOf("broken")
                    result.error shouldBe exception
                    result.error.message shouldBe "Index creation failed"
                    result.shouldBeInstanceOf<MonkoIndexBuilder.EnsureResult.Error>()
                }
            }

            "sealed class hierarchy" - {

                "all subtypes should be EnsureResult" {
                    val repo = TestRepo()

                    val ensured: MonkoIndexBuilder.EnsureResult = MonkoIndexBuilder.EnsureResult.Ensured(
                        repo = repo, name = "a", fields = emptyList(),
                    )
                    val kept: MonkoIndexBuilder.EnsureResult = MonkoIndexBuilder.EnsureResult.Kept(
                        repo = repo, name = "b", fields = emptyList(),
                    )
                    val recreated: MonkoIndexBuilder.EnsureResult = MonkoIndexBuilder.EnsureResult.ReCreated(
                        repo = repo, name = "c", fields = emptyList(),
                    )
                    val error: MonkoIndexBuilder.EnsureResult = MonkoIndexBuilder.EnsureResult.Error(
                        repo = repo, name = "d", fields = emptyList(), error = RuntimeException(),
                    )

                    ensured.shouldBeInstanceOf<MonkoIndexBuilder.EnsureResult>()
                    kept.shouldBeInstanceOf<MonkoIndexBuilder.EnsureResult>()
                    recreated.shouldBeInstanceOf<MonkoIndexBuilder.EnsureResult>()
                    error.shouldBeInstanceOf<MonkoIndexBuilder.EnsureResult>()
                }

                "when expression should work for exhaustive matching" {
                    val repo = TestRepo()

                    val results = listOf(
                        MonkoIndexBuilder.EnsureResult.Ensured(repo = repo, name = "a", fields = emptyList()),
                        MonkoIndexBuilder.EnsureResult.Kept(repo = repo, name = "b", fields = emptyList()),
                        MonkoIndexBuilder.EnsureResult.ReCreated(repo = repo, name = "c", fields = emptyList()),
                        MonkoIndexBuilder.EnsureResult.Error(
                            repo = repo, name = "d", fields = emptyList(), error = RuntimeException(),
                        ),
                    )

                    val names = results.map { result ->
                        when (result) {
                            is MonkoIndexBuilder.EnsureResult.Ensured -> "ensured"
                            is MonkoIndexBuilder.EnsureResult.Kept -> "kept"
                            is MonkoIndexBuilder.EnsureResult.ReCreated -> "recreated"
                            is MonkoIndexBuilder.EnsureResult.Error -> "error"
                        }
                    }

                    names shouldContainExactly listOf("ensured", "kept", "recreated", "error")
                }
            }
        }
    }
}
