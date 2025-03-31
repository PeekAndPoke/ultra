package de.peekandpoke.funktor.insights

import de.peekandpoke.funktor.cluster.depot.repos.fs.FileSystemRepository

class InsightsFileRepository : FileSystemRepository("insights", "./tmp/depot/insights"), InsightsRepository
