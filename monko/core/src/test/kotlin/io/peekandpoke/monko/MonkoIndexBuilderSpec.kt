package io.peekandpoke.monko

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.monko.lang.property
import io.peekandpoke.ultra.reflection.kType
import org.bson.Document

class MonkoIndexBuilderSpec : FreeSpec() {

    data class TestEntity(
        val name: String,
        val age: Int,
        val email: String,
    )

    private class TestRepo : MonkoRepository<TestEntity>(
        name = "test_entities",
        storedType = kType(),
        driver = createMinimalDriver(),
    )

    companion object {
        private fun createMinimalDriver(): MonkoDriver = MonkoDriver(
            lazyCodec = lazy { error("Not needed for index builder tests") },
            lazyClient = lazy { error("Not needed for index builder tests") },
            lazyDatabase = lazy { error("Not needed for index builder tests") },
        )
    }

    init {
        "MonkoIndexBuilder" - {

            "getIndexDefinitions should return empty list when no indexes defined" {
                val repo = TestRepo()
                val builder = MonkoIndexBuilder(repo)

                builder.getIndexDefinitions().shouldBeEmpty()
            }

            "persistentIndex" - {

                "should create a persistent index definition with auto-generated name" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.persistentIndex {
                        field { it.property<String>("name") }
                    }

                    val definitions = builder.getIndexDefinitions()
                    definitions shouldHaveSize 1

                    val indexDef = definitions[0]
                    indexDef.getEffectiveName() shouldBe "persistent-name"
                    indexDef.getFieldPaths() shouldContainExactly listOf("name")
                }

                "should create a persistent index with multiple fields" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.persistentIndex {
                        field { it.property<String>("name") }
                        field { it.property<Int>("age") }
                    }

                    val definitions = builder.getIndexDefinitions()
                    definitions shouldHaveSize 1

                    val indexDef = definitions[0]
                    indexDef.getEffectiveName() shouldBe "persistent-name-age"
                    indexDef.getFieldPaths() shouldContainExactly listOf("name", "age")
                }

                "should support custom name" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.persistentIndex {
                        name("my_custom_index")
                        field { it.property<String>("name") }
                    }

                    val definitions = builder.getIndexDefinitions()
                    definitions shouldHaveSize 1

                    val indexDef = definitions[0]
                    indexDef.getEffectiveName() shouldBe "my_custom_index"
                }

                "getIndexDetails should return correct info" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.persistentIndex {
                        field { it.property<String>("name") }
                    }

                    val details = builder.getIndexDefinitions()[0].getIndexDetails()
                    details.name shouldBe "persistent-name"
                    details.type shouldBe "persistent"
                    details.fields shouldContainExactly listOf("name")
                }
            }

            "uniqueIndex" - {

                "should create a unique index definition" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.uniqueIndex {
                        field { it.property<String>("email") }
                    }

                    val definitions = builder.getIndexDefinitions()
                    definitions shouldHaveSize 1

                    val indexDef = definitions[0]
                    indexDef.getEffectiveName() shouldBe "unique-email"
                    indexDef.getFieldPaths() shouldContainExactly listOf("email")
                }

                "should create a compound unique index" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.uniqueIndex {
                        field { it.property<String>("name") }
                        field { it.property<String>("email") }
                    }

                    val definitions = builder.getIndexDefinitions()
                    definitions shouldHaveSize 1

                    val indexDef = definitions[0]
                    indexDef.getEffectiveName() shouldBe "unique-name-email"
                    indexDef.getFieldPaths() shouldContainExactly listOf("name", "email")
                }

                "getIndexDetails type should be 'unique'" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.uniqueIndex {
                        field { it.property<String>("email") }
                    }

                    val details = builder.getIndexDefinitions()[0].getIndexDetails()
                    details.type shouldBe "unique"
                }

                "should default to non-sparse" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.uniqueIndex {
                        field { it.property<String>("email") }
                    }

                    val indexDef = builder.getIndexDefinitions()[0] as MonkoIndexBuilder.UniqueIndexBuilder<*>
                    indexDef.isSparse() shouldBe false
                }

                "sparse() should mark the index as sparse" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.uniqueIndex {
                        field { it.property<String>("dedupeKey") }
                        sparse()
                    }

                    val indexDef = builder.getIndexDefinitions()[0] as MonkoIndexBuilder.UniqueIndexBuilder<*>
                    indexDef.isSparse() shouldBe true
                }

                "sparse(false) should leave the index non-sparse" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.uniqueIndex {
                        field { it.property<String>("email") }
                        sparse(false)
                    }

                    val indexDef = builder.getIndexDefinitions()[0] as MonkoIndexBuilder.UniqueIndexBuilder<*>
                    indexDef.isSparse() shouldBe false
                }
            }

            "ttlIndex" - {

                "should create a TTL index definition" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.ttlIndex {
                        field { it.property<Long>("age") }
                    }

                    val definitions = builder.getIndexDefinitions()
                    definitions shouldHaveSize 1

                    val indexDef = definitions[0]
                    indexDef.getEffectiveName() shouldBe "ttl-age"
                    indexDef.getFieldPaths() shouldContainExactly listOf("age")
                }

                "should support custom TTL expiration" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.ttlIndex {
                        field { it.property<Long>("age") }
                        expireAfter(3600)
                    }

                    val definitions = builder.getIndexDefinitions()
                    definitions shouldHaveSize 1

                    val indexDef = definitions[0]
                    indexDef.getEffectiveName() shouldBe "ttl-age"
                }

                "getIndexDetails type should be 'ttl'" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.ttlIndex {
                        field { it.property<Long>("age") }
                    }

                    val details = builder.getIndexDefinitions()[0].getIndexDetails()
                    details.type shouldBe "ttl"
                }
            }

            "multiple indexes" - {

                "should accumulate all index definitions" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.persistentIndex {
                        field { it.property<String>("name") }
                    }
                    builder.uniqueIndex {
                        field { it.property<String>("email") }
                    }
                    builder.ttlIndex {
                        field { it.property<Long>("age") }
                    }

                    val definitions = builder.getIndexDefinitions()
                    definitions shouldHaveSize 3
                }
            }

            "matches" - {

                "should match when existing index has the same effective name" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.persistentIndex {
                        field { it.property<String>("name") }
                    }

                    val existing = Document().apply {
                        this["name"] = "persistent-name"
                        this["key"] = Document("name", 1)
                    }

                    builder.getIndexDefinitions()[0].matches(existing) shouldBe true
                }

                "should not match when name differs" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.persistentIndex {
                        field { it.property<String>("name") }
                    }

                    val existing = Document().apply {
                        this["name"] = "some-other-index"
                        this["key"] = Document("name", 1)
                    }

                    builder.getIndexDefinitions()[0].matches(existing) shouldBe false
                }

                "should not match when name field is missing" {
                    val repo = TestRepo()
                    val builder = MonkoIndexBuilder(repo)

                    builder.persistentIndex {
                        field { it.property<String>("name") }
                    }

                    val existing = Document()

                    builder.getIndexDefinitions()[0].matches(existing) shouldBe false
                }
            }

            "dot-separated field names should use underscores in index name" {
                val repo = TestRepo()
                val builder = MonkoIndexBuilder(repo)

                builder.persistentIndex {
                    field {
                        it.property<String>("address").property<String>("city")
                    }
                }

                val indexDef = builder.getIndexDefinitions()[0]
                indexDef.getEffectiveName() shouldContain "address"
                indexDef.getEffectiveName() shouldNotContain "."
            }
        }
    }
}
