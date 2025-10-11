package de.peekandpoke.karango.testdomain

import de.peekandpoke.karango.Karango
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Database
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Repository.Hooks
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.hooks.Timestamped
import de.peekandpoke.ultra.vault.hooks.TimestampedHook

val Database.testTimestamped get() = getRepository<TestTimestampedRepository>()

class TestTimestampedRepository(
    driver: KarangoDriver,
    timestamped: TimestampedHook,
    val testHooks: TestHooks = TestHooks(),
) : EntityRepository<TestTimestamped>(
    name = "test-timestamped",
    storedType = kType(),
    driver = driver,
    hooks = Hooks.of<TestTimestamped>(
        timestamped.onBeforeSave(),
    ).plus(testHooks)
) {
    class TestHooks {
        class OnBeforeSave : Hooks.OnBeforeSave<TestTimestamped> {

            val received: MutableList<Storable<TestTimestamped>> = mutableListOf()

            fun reset() {
                received.clear()
            }

            override fun <X : TestTimestamped> onBeforeSave(
                repo: Repository<TestTimestamped>,
                storable: Storable<TestTimestamped>,
            ): Storable<X> {
                @Suppress("UNCHECKED_CAST")
                return storable.also {
                    received.add(it)
                } as Storable<X>
            }
        }

        class OnAfterSave : Hooks.OnAfterSave<TestTimestamped> {

            val received: MutableList<Storable<TestTimestamped>> = mutableListOf()

            fun reset() {
                received.clear()
            }

            override suspend fun <X : TestTimestamped> onAfterSave(
                repo: Repository<TestTimestamped>,
                stored: Stored<X>,
            ) {
                received.add(stored)
            }
        }

        class OnAfterDelete : Hooks.OnAfterDelete<TestTimestamped> {

            val received: MutableList<Storable<TestTimestamped>> = mutableListOf()

            fun reset() {
                received.clear()
            }

            override suspend fun <X : TestTimestamped> onAfterDelete(
                repo: Repository<TestTimestamped>,
                deleted: Stored<X>,
            ) {
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

private fun Hooks<TestTimestamped>.plus(testHooks: TestTimestampedRepository.TestHooks): Hooks<TestTimestamped> = plus(
    testHooks.onBeforeSave,
    testHooks.onAfterSave,
    testHooks.onAfterDelete,
)

@Karango
data class TestTimestamped(
    val name: String,
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = MpInstant.Epoch,
) : Timestamped {
    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
}
