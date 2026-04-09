package io.peekandpoke.funktor.demo.server.funktorconf

import io.peekandpoke.funktor.core.fixtures.FixtureLoader
import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.funktor.demo.common.funktorconf.SpeakerModel
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoIndexBuilder
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

class SpeakersRepo(
    driver: MonkoDriver,
    timestamped: TimestampedHook,
) : MonkoRepository<Speaker>(
    name = "funktorconf_speakers",
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks
        .of(emptyList<Repository.Hooks.OnAfterSave<Speaker>>())
        .plus(timestamped.onBeforeSave())
) {
    companion object {
        suspend fun Storable<Speaker>.asApiModel() = with(resolve()) {
            SpeakerModel(
                id = _id,
                name = name,
                bio = bio,
                photoUrl = photoUrl,
                talkTitle = talkTitle,
                talkAbstract = talkAbstract,
                createdAt = createdAt.toIsoString(),
                updatedAt = updatedAt.toIsoString(),
            )
        }
    }

    @Suppress("unused", "CanBeParameter")
    class Fixtures(
        repo: SpeakersRepo,
        private val events: EventsRepo.Fixtures,
    ) : RepoFixtureLoader<Speaker>(repo = repo) {

        override val dependsOn: List<FixtureLoader> = listOf(events)

        // Funktor & Ultra ecosystem speakers

        val elenaBrandt = fix {
            "elena-brandt" to Speaker(
                name = "Elena Brandt",
                bio = "Core contributor to the Ultra library suite. Specializes in type-safe DSLs " +
                        "and the Kontainer dependency injection system. Based in Berlin.",
                talkTitle = "Kontainer: DI Without the Magic",
                talkAbstract = "How Kontainer achieves per-request scoping and compile-time safety " +
                        "without annotation processing or reflection tricks. Includes a live refactoring " +
                        "of a Spring app to Kontainer and the surprising places where type-safety catches bugs.",
            )
        }

        val tomaszWielki = fix {
            "tomasz-wielki" to Speaker(
                name = "Tomasz Wielki",
                bio = "Full-stack developer and early adopter of the Kraft SPA framework. " +
                        "Runs a Kotlin/JS consultancy in Warsaw and contributes to the Streams library.",
                talkTitle = "Building SPAs with Kraft",
                talkAbstract = "How to build modern single-page applications entirely in Kotlin. " +
                        "Covers the Kraft component model, Semantic UI DSL, state management with Streams, " +
                        "and why writing your UI in the same language as your backend changes everything.",
            )
        }

        val linaVoss = fix {
            "lina-voss" to Speaker(
                name = "Lina Voss",
                bio = "Backend engineer at a Hamburg fintech. Migrated their persistence layer from " +
                        "raw MongoDB drivers to Monko and Karango in 2025.",
                talkTitle = "Type-Safe Queries with Karango and Monko",
                talkAbstract = "A deep comparison of the Karango (ArangoDB) and Monko (MongoDB) query DSLs. " +
                        "How the @Vault annotation generates type-safe field accessors, and patterns for " +
                        "building complex queries without writing a single raw query string.",
            )
        }

        // Klang Audio speakers

        val raviPatel = fix {
            "ravi-patel" to Speaker(
                name = "Ravi Patel",
                bio = "Audio engineer and Kotlin multiplatform enthusiast. Contributor to the Klang " +
                        "Audio Motör, focusing on the WebAudio worklet integration.",
                talkTitle = "Real-Time Audio in the Browser with Klang",
                talkAbstract = "How the Klang Audio Motör generates and processes audio in real time " +
                        "using Kotlin/JS and WebAudio worklets. Covers the rendering pipeline, " +
                        "oscillator design, and live coding with the Strudel pattern language.",
            )
        }

        val noraEriksson = fix {
            "nora-eriksson" to Speaker(
                name = "Nora Eriksson",
                bio = "Music technologist and KlangScript language designer. Builds creative coding " +
                        "tools in Kotlin and explores the intersection of music and programming.",
                talkTitle = "KlangScript: A DSL for Sound",
                talkAbstract = "Designing a domain-specific language for audio synthesis. How KlangScript's " +
                        "parser and interpreter work under the hood, the block editor for visual patching, " +
                        "and what it means to treat sound as a first-class data type.",
            )
        }

        // Serialization & tooling speakers

        val marcAurel = fix {
            "marc-aurel" to Speaker(
                name = "Marc Aurel",
                bio = "Open-source maintainer and serialization nerd. Has contributed to Slumber, " +
                        "the Ultra serialization library, and writes about Kotlin codegen.",
                talkTitle = "Slumber: Serialization Without Surprises",
                talkAbstract = "Why another serialization library? This talk covers Slumber's codec system, " +
                        "polymorphic type handling, and how it integrates with Kontainer and Funktor's REST layer. " +
                        "Includes gotchas from migrating a production API from kotlinx.serialization.",
            )
        }

        val junKato = fix {
            "jun-kato" to Speaker(
                name = "Jun Kato",
                bio = "Platform engineer specializing in Kotlin multiplatform builds and KSP code generation. " +
                        "Maintains Mutator, the KSP-powered immutable data class wrapper generator.",
                talkTitle = "Mutator and KSP: Generating Code That Writes Itself",
                talkAbstract = "A peek behind the curtain of KSP symbol processing in the Ultra ecosystem. " +
                        "How Mutator generates mutable wrappers, how @Vault creates query DSL accessors, " +
                        "and practical tips for writing your own KSP processors.",
            )
        }
    }

    override fun MonkoIndexBuilder<Speaker>.buildIndexes() {
        persistentIndex {
            field { it.name }
        }
    }
}
