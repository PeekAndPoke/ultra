package io.peekandpoke.funktor.insights

import io.peekandpoke.funktor.cluster.depot.repos.fs.FileSystemRepository

/**
 * Default file-system-backed repository for storing insights data.
 *
 * [dir] is resolved against the working directory when relative, so the default puts records under the
 * directory the app was started from. A test must pass an absolute temp directory: otherwise it reads
 * whatever a previous demo run left behind, and behaves differently per machine.
 */
class InsightsFileRepository(dir: String = DEFAULT_DIR) :
    FileSystemRepository("insights", dir), InsightsRepository {

    companion object {
        const val DEFAULT_DIR = "./tmp/depot/insights"
    }
}
