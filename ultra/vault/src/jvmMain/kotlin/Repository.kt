package de.peekandpoke.ultra.vault

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.polymorphism.PolymorphicParentUtil
import de.peekandpoke.ultra.vault.lang.Aliased
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.IterableExpr
import de.peekandpoke.ultra.vault.lang.Printer
import kotlinx.coroutines.delay
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier

interface Repository<T : Any> : Expression<List<T>>, Aliased {

    class Hooks<T : Any>(
        val onBeforeSave: List<OnBeforeSave<T>>,
        val onAfterSave: List<OnAfterSave<T>>,
        val onAfterDelete: List<OnAfterDelete<T>>,
    ) {
        companion object {
            fun <T : Any> of(vararg hooks: Hook<T>) = of(hooks.toList())

            fun <T : Any> of(hooks: List<Hook<T>>) = Hooks(
                onBeforeSave = hooks.filterIsInstance<OnBeforeSave<T>>(),
                onAfterSave = hooks.filterIsInstance<OnAfterSave<T>>(),
                onAfterDelete = hooks.filterIsInstance<OnAfterDelete<T>>(),
            )

            fun <T : Any> empty() = Hooks<T>(
                onBeforeSave = emptyList(),
                onAfterSave = emptyList(),
                onAfterDelete = emptyList(),
            )
        }

        interface Hook<T : Any>

        interface OnBeforeSave<T : Any> : Hook<T> {
            fun <X : T> onBeforeSave(repo: Repository<T>, storable: Storable<T>): Storable<X>
        }

        interface OnAfterSave<T : Any> : Hook<T> {
            suspend fun <X : T> onAfterSave(repo: Repository<T>, stored: Stored<X>)
        }

        interface OnAfterDelete<T : Any> : Hook<T> {
            suspend fun <X : T> onAfterDelete(repo: Repository<T>, deleted: Stored<X>)
        }

        fun plus(hook: Hook<T>): Hooks<T> = plus(of(hook))

        fun plus(other: Hooks<T>): Hooks<T> = Hooks(
            onBeforeSave = this.onBeforeSave.plus(other.onBeforeSave),
            onAfterSave = this.onAfterSave.plus(other.onAfterSave),
            onAfterDelete = this.onAfterDelete.plus(other.onAfterDelete),
        )

        fun plus(vararg hooks: List<Hook<T>>): Hooks<T> = plus(
            of(
                hooks.toList().flatten()
            )
        )

        fun <X : T> applyOnBeforeSaveHooks(repo: Repository<T>, storable: Storable<X>): Storable<X> {
            return onBeforeSave.fold(storable) { acc, hook -> hook.onBeforeSave(repo, acc) }
        }

        fun <X : T> applyOnAfterSaveHooks(repo: Repository<T>, stored: Stored<X>): Stored<X> {
            Vault.launch {
                delay(1)
                onAfterSave.forEach { hook -> hook.onAfterSave(repo, stored) }
            }

            return stored
        }

        fun <X : T> applyOnAfterDeleteHooks(repo: Repository<T>, stored: Stored<X>): Stored<X> {
            Vault.launch {
                delay(1)
                onAfterDelete.forEach { hook -> hook.onAfterDelete(repo, stored) }
            }

            return stored
        }
    }

    /**
     * The name of the repository
     */
    val name: String

    /**
     * The connection identifier of the repository
     */
    val connection: String

    /**
     * The type of the entities stored in the repository.
     *
     * This is needed for deserialization
     */
    val storedType: TypeRef<T>

    /**
     * Helper for accessing the repos this pointer
     */
    val repo: Repository<T> get() = this

    /**
     * Gets an iterable expression for this repo
     */
    fun asIterableExpr(rootName: String = "root"): IterableExpr<T> = IterableExpr("root", this)

    /**
     * Returns the expression type of the repo
     */
    override fun getType(): TypeRef<List<T>> = storedType.list

    /**
     * Prints the repo as part of a query
     */
    override fun print(p: Printer) {
        p.name(name)
    }

    /**
     * Gets the alias
     */
    override fun getAlias(): String {
        return name
    }

    /**
     * Ensures that the repository is set up properly
     *
     * Makes sure that:
     * 1. the repository is created in the database
     * 2. indexes are set up
     *
     * @see ensureRepository
     * @see ensureIndexes
     */
    suspend fun ensure() {
        ensureRepository()
        ensureIndexes()
    }

    /**
     * Ensures that the repository is created in the database
     */
    suspend fun ensureRepository() {}

    /**
     * Gets figures about the repository
     */
    suspend fun getStats(): VaultModels.RepositoryStats = VaultModels.RepositoryStats.empty

    /**
     * Validates that all indexes are set properly and that there are not excess indexes
     */
    suspend fun validateIndexes(): VaultModels.IndexesInfo = VaultModels.IndexesInfo.empty

    /**
     * Ensures that the indexes are set up
     */
    suspend fun ensureIndexes() {}

    /**
     * Drops all indexes and then re-creates them.
     */
    suspend fun recreateIndexes() {}

    /**
     * Returns a set of all entity classes that the repository stored.
     *
     * This will include the base type [storedType] and all or its registered
     * polymorphic children.
     */
    fun getAllStoredClasses(): Set<KClass<out T>> {
        @Suppress("UNCHECKED_CAST")
        val mainClass = storedType.type.classifier as KClass<out T>

        return listOf(mainClass)
            .plus(PolymorphicParentUtil.getChildren(mainClass))
            .toSet()
    }

    /**
     * Checks whether the repository stores the given cls
     */
    fun stores(type: KClassifier): Boolean {
        // Is this the exact type that we store?
        return type == storedType.type.classifier ||
                // Or is the stored type a super type?
                storedType.type.classifier in (type as KClass<*>).supertypes.map { it.classifier }
    }

    /**
     * Ensure that the given [idOrKey] is an ID in the form <NAME>/<KEY>
     */
    fun ensureId(idOrKey: String): String {
        return if (idOrKey.contains("/")) {
            idOrKey
        } else {
            "$name/$idOrKey"
        }
    }

    /**
     * Returns all documents in the repository.
     */
    suspend fun findAll(): Cursor<Stored<T>> = Cursor.empty()

    /**
     * Retrieves a document by id or null if there is none.
     */
    suspend fun findById(id: String?): Stored<T>?

    /**
     * Inserts the given object into the database and returns the saved version
     */
    suspend fun <X : T> insert(new: X): Stored<X> = insert(New(new))

    /**
     * Inserts the given object into the database and returns the saved version
     */
    suspend fun <X : T> insert(new: X, modify: suspend (X) -> X): Stored<X> = insert(New(modify(new)))

    /**
     * Inserts the given object with the given key into the database and returns the saved version
     */
    suspend fun <X : T> insert(key: String?, new: X): Stored<X> = when (key) {
        null -> insert(new)
        else -> insert(New(_key = key, value = new))
    }

    /**
     * Inserts the given object into the database and returns the saved version
     */
    suspend fun <X : T> insert(new: New<X>): Stored<X>

    /**
     * Tries to insert the given [entry].
     *
     * Return the stored entry, if the insert is successful.
     */
    suspend fun <X : T> tryInsert(entry: X): Stored<X>? {
        return try {
            insert(entry)
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Updates the given obj in the database and returns the saved version
     */
    suspend fun <X : T> save(stored: Stored<X>): Stored<X>

    /**
     * Saves the given storable and returns the saved version
     */
    suspend fun <X : T> save(storable: Storable<X>): Stored<X> = when (storable) {
        is New<X> -> insert(storable)

        is Stored<X> -> save(storable)

        is Ref<X> -> save(storable.asStored)

        is LazyRef<X> -> save(storable.asStored)
    }

    /**
     * Save the given [storable] and applying the given [modify] before storing it.
     *
     * Returns the saved version
     */
    suspend fun <IN : T, OUT : T> save(storable: Storable<IN>, modify: suspend (IN) -> OUT): Stored<OUT> {
        val modified: Storable<OUT> = (storable as Storable<T>).withValue(
            modify(storable.value)
        )

        return save(modified)
    }

    /**
     * Save the given [stored] and applying the given [modify] before storing it.
     *
     * Returns the saved version
     */
    suspend fun <X : T> saveIfModified(stored: Storable<X>, modify: suspend (X) -> X): Stored<X> {

        val originalValue = stored.value
        val modifiedValue = modify(originalValue)

        return when (originalValue != modifiedValue) {
            true -> save(stored.withValue(modifiedValue))
            else -> stored.asStored
        }
    }

    /**
     * Removes the given entity
     */
    suspend fun <X : T> remove(entity: Stored<X>): RemoveResult = remove(entity._id)

    /**
     * Remove the document with the given id or key
     */
    suspend fun remove(idOrKey: String): RemoveResult

    /**
     * Remove all entries from the collection
     */
    suspend fun removeAll(): RemoveResult
}
