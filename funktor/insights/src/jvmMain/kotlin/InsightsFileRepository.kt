package io.peekandpoke.funktor.insights

import io.peekandpoke.funktor.cluster.depot.repos.fs.FileSystemRepository

/** Default file-system-backed repository for storing insights data. */
class InsightsFileRepository : FileSystemRepository("insights", "./tmp/depot/insights"), InsightsRepository
