package de.peekandpoke.ultra.playground

import de.peekandpoke.monko.MonkoConfig
import de.peekandpoke.monko.MonkoDriver
import de.peekandpoke.monko.MonkoRepository
import de.peekandpoke.monko.monko
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.kontainer.kontainer
import de.peekandpoke.ultra.log.ultraLogging
import de.peekandpoke.ultra.vault.ultraVault
import io.github.serpro69.kfaker.faker

data class Movie(val title: String, val year: Int)

class MoviesRepository(
    driver: MonkoDriver,
) : MonkoRepository<Movie>(
    name = "movies",
    storedType = kType(),
    driver = driver,
)

suspend fun main() {

    val config = MonkoConfig(
        connectionString = "mongodb://root:root@localhost:27017",
        database = "funktor-dev",
    )

    val blueprint = kontainer {
        singleton(Kronos::class) { Kronos.systemUtc }

        ultraLogging()
        ultraVault()
        monko(config)

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

    val loaded = moviesRepository.findAll()

    println("Found documents: ${loaded.count}")

    loaded.forEachIndexed { idx, movie ->
        println("Found document ${idx + 1}: $movie")
    }
}
