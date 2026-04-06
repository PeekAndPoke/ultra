package io.peekandpoke.monko

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.Stored

class MonkoRepositoryHooksSpec : FreeSpec() {

    data class TestEntity(
        val name: String,
        val counter: Int = 0,
    )

    init {
        "Repository.Hooks" - {

            "empty hooks" - {

                "should have no onBeforeSave hooks" {
                    val hooks = Repository.Hooks.empty<TestEntity>()

                    hooks.onBeforeSave.shouldBeEmpty()
                }

                "should have no onAfterSave hooks" {
                    val hooks = Repository.Hooks.empty<TestEntity>()

                    hooks.onAfterSave.shouldBeEmpty()
                }

                "should have no onAfterDelete hooks" {
                    val hooks = Repository.Hooks.empty<TestEntity>()

                    hooks.onAfterDelete.shouldBeEmpty()
                }
            }

            "of with OnBeforeSave hook" - {

                "should collect before-save hooks" {
                    val hook = object : Repository.Hooks.OnBeforeSave<TestEntity> {
                        @Suppress("UNCHECKED_CAST")
                        override fun <X : TestEntity> onBeforeSave(
                            repo: Repository<TestEntity>,
                            storable: Storable<TestEntity>,
                        ): Storable<X> {
                            return storable.modify { it.copy(counter = it.counter + 1) } as Storable<X>
                        }
                    }

                    val hooks = Repository.Hooks.of(hook)

                    hooks.onBeforeSave shouldHaveSize 1
                    hooks.onAfterSave.shouldBeEmpty()
                    hooks.onAfterDelete.shouldBeEmpty()
                }
            }

            "of with OnAfterSave hook" - {

                "should collect after-save hooks" {
                    val hook = object : Repository.Hooks.OnAfterSave<TestEntity> {
                        override suspend fun <X : TestEntity> onAfterSave(
                            repo: Repository<TestEntity>,
                            stored: Stored<X>,
                        ) {
                            // no-op for test
                        }
                    }

                    val hooks = Repository.Hooks.of(hook)

                    hooks.onBeforeSave.shouldBeEmpty()
                    hooks.onAfterSave shouldHaveSize 1
                    hooks.onAfterDelete.shouldBeEmpty()
                }
            }

            "of with OnAfterDelete hook" - {

                "should collect after-delete hooks" {
                    val hook = object : Repository.Hooks.OnAfterDelete<TestEntity> {
                        override suspend fun <X : TestEntity> onAfterDelete(
                            repo: Repository<TestEntity>,
                            deleted: Stored<X>,
                        ) {
                            // no-op for test
                        }
                    }

                    val hooks = Repository.Hooks.of(hook)

                    hooks.onBeforeSave.shouldBeEmpty()
                    hooks.onAfterSave.shouldBeEmpty()
                    hooks.onAfterDelete shouldHaveSize 1
                }
            }

            "plus should combine hooks" - {

                "should merge two hooks collections" {
                    val beforeHook = object : Repository.Hooks.OnBeforeSave<TestEntity> {
                        @Suppress("UNCHECKED_CAST")
                        override fun <X : TestEntity> onBeforeSave(
                            repo: Repository<TestEntity>,
                            storable: Storable<TestEntity>,
                        ): Storable<X> = storable as Storable<X>
                    }

                    val afterHook = object : Repository.Hooks.OnAfterSave<TestEntity> {
                        override suspend fun <X : TestEntity> onAfterSave(
                            repo: Repository<TestEntity>,
                            stored: Stored<X>,
                        ) {
                        }
                    }

                    val hooks1 = Repository.Hooks.of<TestEntity>(beforeHook)
                    val hooks2 = Repository.Hooks.of<TestEntity>(afterHook)

                    val combined = hooks1.plus(hooks2)

                    combined.onBeforeSave shouldHaveSize 1
                    combined.onAfterSave shouldHaveSize 1
                }

                "should accumulate multiple hooks of the same type" {
                    val hook1 = object : Repository.Hooks.OnBeforeSave<TestEntity> {
                        @Suppress("UNCHECKED_CAST")
                        override fun <X : TestEntity> onBeforeSave(
                            repo: Repository<TestEntity>,
                            storable: Storable<TestEntity>,
                        ): Storable<X> = storable as Storable<X>
                    }

                    val hook2 = object : Repository.Hooks.OnBeforeSave<TestEntity> {
                        @Suppress("UNCHECKED_CAST")
                        override fun <X : TestEntity> onBeforeSave(
                            repo: Repository<TestEntity>,
                            storable: Storable<TestEntity>,
                        ): Storable<X> = storable as Storable<X>
                    }

                    val hooks1 = Repository.Hooks.of<TestEntity>(hook1)
                    val hooks2 = Repository.Hooks.of<TestEntity>(hook2)

                    val combined = hooks1.plus(hooks2)

                    combined.onBeforeSave shouldHaveSize 2
                }
            }

            "applyOnBeforeSaveHooks" - {

                "should apply the before-save hook to a New entity" {
                    val hook = object : Repository.Hooks.OnBeforeSave<TestEntity> {
                        @Suppress("UNCHECKED_CAST")
                        override fun <X : TestEntity> onBeforeSave(
                            repo: Repository<TestEntity>,
                            storable: Storable<TestEntity>,
                        ): Storable<X> {
                            return storable.modify { it.copy(counter = it.counter + 1) } as Storable<X>
                        }
                    }

                    val hooks = Repository.Hooks.of(hook)

                    val entity = New(value = TestEntity(name = "test", counter = 0))

                    // We need a repo to apply hooks. Use a mock-like approach
                    // Since we can't easily create a MonkoRepository here without a driver,
                    // we test the hooks.applyOnBeforeSaveHooks method directly
                    val fakeRepo = object : Repository<TestEntity> {
                        override val name = "test"
                        override val connection = "test"
                        override val storedType = kType<TestEntity>()
                        override suspend fun findById(id: String?) = null
                        override suspend fun <X : TestEntity> insert(new: New<X>) = error("not implemented")
                        override suspend fun <X : TestEntity> save(stored: Stored<X>) = error("not implemented")
                        override suspend fun remove(idOrKey: String) = error("not implemented")
                        override suspend fun removeAll() = error("not implemented")
                    }

                    val result = hooks.applyOnBeforeSaveHooks(fakeRepo, entity)

                    result.value.counter shouldBe 1
                }

                "should chain multiple before-save hooks" {
                    val hook1 = object : Repository.Hooks.OnBeforeSave<TestEntity> {
                        @Suppress("UNCHECKED_CAST")
                        override fun <X : TestEntity> onBeforeSave(
                            repo: Repository<TestEntity>,
                            storable: Storable<TestEntity>,
                        ): Storable<X> {
                            return storable.modify { it.copy(counter = it.counter + 1) } as Storable<X>
                        }
                    }

                    val hook2 = object : Repository.Hooks.OnBeforeSave<TestEntity> {
                        @Suppress("UNCHECKED_CAST")
                        override fun <X : TestEntity> onBeforeSave(
                            repo: Repository<TestEntity>,
                            storable: Storable<TestEntity>,
                        ): Storable<X> {
                            return storable.modify { it.copy(counter = it.counter * 10) } as Storable<X>
                        }
                    }

                    val hooks = Repository.Hooks.of(hook1, hook2)
                    val entity = New(value = TestEntity(name = "test", counter = 0))

                    val fakeRepo = object : Repository<TestEntity> {
                        override val name = "test"
                        override val connection = "test"
                        override val storedType = kType<TestEntity>()
                        override suspend fun findById(id: String?) = null
                        override suspend fun <X : TestEntity> insert(new: New<X>) = error("not implemented")
                        override suspend fun <X : TestEntity> save(stored: Stored<X>) = error("not implemented")
                        override suspend fun remove(idOrKey: String) = error("not implemented")
                        override suspend fun removeAll() = error("not implemented")
                    }

                    val result = hooks.applyOnBeforeSaveHooks(fakeRepo, entity)

                    // hook1: 0 + 1 = 1, then hook2: 1 * 10 = 10
                    result.value.counter shouldBe 10
                }
            }

            "empty hooks should pass through unchanged" {
                val hooks = Repository.Hooks.empty<TestEntity>()

                val entity = New(value = TestEntity(name = "original", counter = 42))

                val fakeRepo = object : Repository<TestEntity> {
                    override val name = "test"
                    override val connection = "test"
                    override val storedType = kType<TestEntity>()
                    override suspend fun findById(id: String?) = null
                    override suspend fun <X : TestEntity> insert(new: New<X>) = error("not implemented")
                    override suspend fun <X : TestEntity> save(stored: Stored<X>) = error("not implemented")
                    override suspend fun remove(idOrKey: String) = error("not implemented")
                    override suspend fun removeAll() = error("not implemented")
                }

                val result = hooks.applyOnBeforeSaveHooks(fakeRepo, entity)

                result.value shouldBe entity.value
            }
        }
    }
}
