package io.peekandpoke.funktor.cluster.depot

import io.peekandpoke.ultra.depot.domain.DepotItem
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.repos.DepotRepository
import io.peekandpoke.ultra.vault.Storable

/**
 * The stored entity as the data subject of a depot file: its database id, `collection/key`.
 * The id is taken as it is: a reference woken from a client's input names whatever collection the client wrote, so
 * check or load it first.
 *
 * @throws IllegalArgumentException for an entity that is not stored yet (no collection or key)
 */
inline fun <reified T : Any> Storable<T>.asDepotSubject(): DepotSubjectId<T> =
    DepotSubjectId.of(type = T::class, namespace = collection, id = _key)

/** Puts a file for the stored entity it is about; see [asDepotSubject]. */
suspend inline fun <reified T : Any> DepotRepository<T>.putFile(
    subject: Storable<T>,
    path: String,
    content: ByteArray,
    meta: Map<String, String> = emptyMap(),
): DepotItem = putFile(subject = subject.asDepotSubject(), path = path, content = content, meta = meta)

/** Puts a file for the stored entity it is about; see [asDepotSubject]. */
suspend inline fun <reified T : Any> DepotRepository<T>.putFile(
    subject: Storable<T>,
    path: String,
    content: String,
    meta: Map<String, String> = emptyMap(),
): DepotItem = putFile(subject = subject.asDepotSubject(), path = path, content = content, meta = meta)
