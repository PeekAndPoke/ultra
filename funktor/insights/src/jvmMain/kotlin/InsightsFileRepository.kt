package io.peekandpoke.funktor.insights

import io.peekandpoke.funktor.cluster.depot.repos.fs.FileSystemRepository

class InsightsFileRepository : FileSystemRepository("insights", "./tmp/depot/insights"), InsightsRepository
