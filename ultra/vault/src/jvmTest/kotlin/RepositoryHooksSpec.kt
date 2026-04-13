package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

class RepositoryHooksSpec : StringSpec({

    // Hooks.empty /////////////////////////////////////////////////////////////////////////////////

    "Hooks.empty has no hooks of any kind" {
        val hooks = Repository.Hooks.empty<String>()

        hooks.onBeforeSave.shouldBeEmpty()
        hooks.onAfterSave.shouldBeEmpty()
        hooks.onAfterDelete.shouldBeEmpty()
    }

    // Hooks.of ////////////////////////////////////////////////////////////////////////////////////

    "Hooks.of with OnBeforeSave hook categorizes correctly" {
        val hook = object : Repository.Hooks.OnBeforeSave<String> {
            override fun <X : String> onBeforeSave(repo: Repository<String>, storable: Storable<String>): Storable<X> {
                @Suppress("UNCHECKED_CAST")
                return storable as Storable<X>
            }
        }

        val hooks = Repository.Hooks.of(hook)

        hooks.onBeforeSave shouldHaveSize 1
        hooks.onAfterSave.shouldBeEmpty()
        hooks.onAfterDelete.shouldBeEmpty()
    }

    "Hooks.of with OnAfterSave hook categorizes correctly" {
        val hook = object : Repository.Hooks.OnAfterSave<String> {
            override suspend fun <X : String> onAfterSave(repo: Repository<String>, stored: Stored<X>) {}
        }

        val hooks = Repository.Hooks.of(hook)

        hooks.onBeforeSave.shouldBeEmpty()
        hooks.onAfterSave shouldHaveSize 1
        hooks.onAfterDelete.shouldBeEmpty()
    }

    "Hooks.of with OnAfterDelete hook categorizes correctly" {
        val hook = object : Repository.Hooks.OnAfterDelete<String> {
            override suspend fun <X : String> onAfterDelete(repo: Repository<String>, deleted: Stored<X>) {}
        }

        val hooks = Repository.Hooks.of(hook)

        hooks.onBeforeSave.shouldBeEmpty()
        hooks.onAfterSave.shouldBeEmpty()
        hooks.onAfterDelete shouldHaveSize 1
    }

    // Hooks.plus //////////////////////////////////////////////////////////////////////////////////

    "Hooks.plus(hook) appends a single hook" {
        val hooks = Repository.Hooks.empty<String>()

        val hook = object : Repository.Hooks.OnAfterSave<String> {
            override suspend fun <X : String> onAfterSave(repo: Repository<String>, stored: Stored<X>) {}
        }

        val combined = hooks.plus(hook)

        combined.onAfterSave shouldHaveSize 1
        combined.onBeforeSave.shouldBeEmpty()
    }

    "Hooks.plus(other) merges two Hooks instances" {
        val hook1 = object : Repository.Hooks.OnBeforeSave<String> {
            override fun <X : String> onBeforeSave(repo: Repository<String>, storable: Storable<String>): Storable<X> {
                @Suppress("UNCHECKED_CAST")
                return storable as Storable<X>
            }
        }

        val hook2 = object : Repository.Hooks.OnBeforeSave<String> {
            override fun <X : String> onBeforeSave(repo: Repository<String>, storable: Storable<String>): Storable<X> {
                @Suppress("UNCHECKED_CAST")
                return storable as Storable<X>
            }
        }

        val hooks1 = Repository.Hooks.of(hook1)
        val hooks2 = Repository.Hooks.of(hook2)
        val combined = hooks1.plus(hooks2)

        combined.onBeforeSave shouldHaveSize 2
    }

    // applyOnBeforeSaveHooks //////////////////////////////////////////////////////////////////////

    "applyOnBeforeSaveHooks with no hooks returns original storable" {
        val hooks = Repository.Hooks.empty<String>()
        val storable = New("hello")
        val repo = TestHooksRepo()

        val result = hooks.applyOnBeforeSaveHooks(repo, storable)

        result.resolve() shouldBe "hello"
    }

    "applyOnBeforeSaveHooks applies hooks in order" {
        val hook = object : Repository.Hooks.OnBeforeSave<String> {
            override fun <X : String> onBeforeSave(repo: Repository<String>, storable: Storable<String>): Storable<X> {
                @Suppress("UNCHECKED_CAST")
                return storable.withValue(storable.valueInternal.uppercase()) as Storable<X>
            }
        }

        val hooks = Repository.Hooks.of(hook)
        val storable = New("hello")
        val repo = TestHooksRepo()

        val result = hooks.applyOnBeforeSaveHooks(repo, storable)

        result.resolve() shouldBe "HELLO"
    }
})

// Test fixtures ///////////////////////////////////////////////////////////////////////////////////

private class TestHooksRepo : Repository<String> {
    override val name: String = "test_hooks"
    override val connection: String = "default"
    override val storedType: TypeRef<String> = kType()

    override suspend fun findById(id: String?): Stored<String>? = null
    override suspend fun <X : String> insert(new: New<X>): Stored<X> = error("not implemented")
    override suspend fun <X : String> save(stored: Stored<X>): Stored<X> = error("not implemented")
    override suspend fun remove(idOrKey: String): RemoveResult = RemoveResult.empty
    override suspend fun removeAll(): RemoveResult = RemoveResult.empty
}
