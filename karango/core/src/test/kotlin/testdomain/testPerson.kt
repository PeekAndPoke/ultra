package de.peekandpoke.karango.testdomain

import de.peekandpoke.karango.Karango
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Database
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Repository.Hooks
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.Stored

val Database.testPersons get() = getRepository<TestPersonsRepository>()

class TestPersonsRepository(
    driver: KarangoDriver,
    val testHooks: TestHooks = TestHooks(),
) : EntityRepository<TestPerson>(
    name = "test-persons",
    storedType = kType(),
    driver = driver,
    hooks = Hooks.of<TestPerson>().plus(testHooks)
) {

    class TestHooks {
        class OnBeforeSave : Hooks.OnBeforeSave<TestPerson> {

            val received: MutableList<Storable<TestPerson>> = mutableListOf()

            fun reset() {
                received.clear()
            }

            override fun <X : TestPerson> onBeforeSave(
                repo: Repository<TestPerson>,
                storable: Storable<TestPerson>,
            ): Storable<X> {
                @Suppress("UNCHECKED_CAST")
                return storable.also {
                    received.add(it)
                } as Storable<X>
            }
        }

        class OnAfterSave : Hooks.OnAfterSave<TestPerson> {

            val received: MutableList<Storable<TestPerson>> = mutableListOf()

            fun reset() {
                received.clear()
            }

            override suspend fun <X : TestPerson> onAfterSave(repo: Repository<TestPerson>, stored: Stored<X>) {
                received.add(stored)
            }
        }

        class OnAfterDelete : Hooks.OnAfterDelete<TestPerson> {

            val received: MutableList<Storable<TestPerson>> = mutableListOf()

            fun reset() {
                received.clear()
            }

            override suspend fun <X : TestPerson> onAfterDelete(repo: Repository<TestPerson>, deleted: Stored<X>) {
                received.add(deleted)
            }
        }

        val onBeforeSave = listOf(OnBeforeSave(), OnBeforeSave())
        val onAfterSave = listOf(OnAfterSave(), OnAfterSave())
        val onAfterDelete = listOf(OnAfterDelete(), OnAfterDelete())

        fun reset() {
            onBeforeSave.forEach { it.reset() }
            onAfterSave.forEach { it.reset() }
            onAfterDelete.forEach { it.reset() }
        }
    }
}

private fun Hooks<TestPerson>.plus(testHooks: TestPersonsRepository.TestHooks): Hooks<TestPerson> = plus(
    testHooks.onBeforeSave,
    testHooks.onAfterSave,
    testHooks.onAfterDelete,
)

@Karango
data class TestPerson(
    val name: String,
    val details: TestPersonDetails = TestPersonDetails(),
    val addresses: List<TestAddress> = emptyList(),
    val books: List<TestBook> = emptyList(),
)

data class TestPersonDetails(
    val title: String? = null,
    val middleName: String = "",
)

data class TestAddress(
    val street: String,
    val number: String,
    val city: String,
    val zip: String,
)

data class TestBook(
    val title: String,
    val authors: List<TestAuthor> = listOf(),
)

data class TestAuthor(
    val firstName: String,
    val lastName: String,
)
