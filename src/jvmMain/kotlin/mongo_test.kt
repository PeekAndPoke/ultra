package io.peekandpoke.ultra.playground

import com.mongodb.client.model.Filters
import io.github.serpro69.kfaker.faker
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.log.ultraLogging
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.VaultConfig
import io.peekandpoke.ultra.vault.ultraVault

data class Movie(val title: String, val year: Int)

class MoviesRepository(
    driver: MonkoDriver,
) : MonkoRepository<Movie>(
    name = "movies",
    storedType = kType(),
    driver = driver,
)

suspend fun main() {

    val vaultConfig = VaultConfig()

    val mongoConfig = MongoDbConfig(
        connectionString = "mongodb://root:root@localhost:27017",
        database = "funktor-dev",
    )

    val blueprint = kontainer {
        singleton(Kronos::class) { Kronos.systemUtc }

        ultraLogging()
        ultraVault(vaultConfig)
        monko(mongoConfig)

        dynamic(MoviesRepository::class)
    }

    val di = blueprint.create()

    val moviesRepository = di.get<MoviesRepository>()

    val faker = faker { }

    val new = Movie(
        title = faker.movie.title(),
        year = faker.random.nextInt(1900, 2023),
    )

    val inserted = moviesRepository.insert(new)

    println("Inserted document: $inserted")

    val loaded = moviesRepository.find {
        filter(
            Filters.regex("title", "a")
        )
        limit(10)
    }

    println("Query:")
    println(loaded.query.query)

    println()
    println("Found documents: ${loaded.count}")

    loaded.forEachIndexed { idx, movie ->
        println("Found document ${idx + 1}: $movie")
    }

    println()
}
