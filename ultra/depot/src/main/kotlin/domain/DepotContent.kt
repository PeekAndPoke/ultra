package io.peekandpoke.ultra.depot.domain

/** What reading a file's content gave. */
sealed interface DepotContent {
    /** The file's content. */
    class Found(val bytes: ByteArray) : DepotContent

    /** There is no such file. */
    data object NotFound : DepotContent

    /** The file is there and can never be read again: the key of its data subject was erased. */
    data object Shredded : DepotContent

    /** The content, or null when there is none to read. */
    fun bytesOrNull(): ByteArray? = (this as? Found)?.bytes
}
