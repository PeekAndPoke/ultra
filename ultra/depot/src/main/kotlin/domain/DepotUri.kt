package io.peekandpoke.ultra.depot.domain

/**
 * Structured URI pointing to a file in a depot repository (e.g. `depot://repo-name/path/to/file`).
 *
 * It names exactly one path: the [path] is kept as it is given, never trimmed or tidied, so two files never share
 * one uri. A [repo] name has no `/` or `\` and no blanks around it, so the uri reads back one way only.
 */
@Suppress("DataClassPrivateConstructor", "DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING")
data class DepotUri private constructor(
    val repo: String,
    val path: String,
) {
    companion object {

        const val protocol = "depot"

        private val parseRegex = "($protocol://)([^/]+)/(.+)".toRegex(RegexOption.DOT_MATCHES_ALL)

        /** Whether [name] is a repository name a uri can carry: not blank, no `/` or `\`, no blanks around it. */
        fun isRepoName(name: String): Boolean =
            name.isNotBlank() && name == name.trim() && '/' !in name && Char(0x5C) !in name

        /**
         * The uri of [path] in the repository [repo], the path as it is.
         *
         * @throws IllegalArgumentException for a [repo] that is no repository name, see [isRepoName]
         */
        operator fun invoke(repo: String, path: String): DepotUri {
            require(isRepoName(repo)) { "Invalid depot repository name" }

            return DepotUri(repo = repo, path = path)
        }

        fun of(item: DepotItem): DepotUri = invoke(repo = item.repo.name, path = item.path)

        /** Reads a uri as [uri] printed it; null for anything else. */
        fun parse(uri: String): DepotUri? {
            val result = parseRegex.matchEntire(uri) ?: return null
            val repo = result.groupValues[2]

            if (!isRepoName(repo)) return null

            return DepotUri(repo = repo, path = result.groupValues[3])
        }
    }

    val uri: String = "$protocol://$repo/$path"
}
