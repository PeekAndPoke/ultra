package de.peekandpoke.ktorfx.cluster.depot.domain

import de.peekandpoke.ktorfx.cluster.depot.api.DepotUriModel

@Suppress("DataClassPrivateConstructor", "DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING")
data class DepotUri private constructor(
    val repo: String,
    val path: String,
) {
    companion object {

        const val protocol = DepotUriModel.protocol

        private val parseRegex = "($protocol://)([^/]+)/(.+)".toRegex()

        private val slashesRegex = "[/\\\\]+".toRegex()

        operator fun invoke(repo: String, path: String) = DepotUri(
            repo = repo.trim().trim('/').replace(slashesRegex, "-"),
            path = path.trim().trimStart('/').replace(slashesRegex, "/"),
        )

        fun of(item: DepotItem): DepotUri = DepotUri(
            repo = item.repo.name,
            path = item.path,
        )

        fun parse(uri: String): DepotUri? {
            val result = parseRegex.matchEntire(uri) ?: return null

            return invoke(
                repo = result.groupValues[2],
                path = result.groupValues[3],
            )
        }
    }

    val uri: String = "$protocol://${repo}/${path}"

    fun asApiModel() = DepotUriModel(repo = repo, path = path, uri = uri)
}
