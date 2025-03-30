package de.peekandpoke.ktorfx.insights

import de.peekandpoke.ktorfx.cluster.depot.repos.fs.FileSystemRepository

class InsightsFileRepository : FileSystemRepository("insights", "./tmp/depot/insights"), InsightsRepository
