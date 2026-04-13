package io.peekandpoke.funktor.demo.server.funktorconf

import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.funktor.demo.common.funktorconf.EventModel
import io.peekandpoke.funktor.demo.common.funktorconf.EventStatus
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoIndexBuilder
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

class EventsRepo(
    driver: MonkoDriver,
    timestamped: TimestampedHook,
) : MonkoRepository<Event>(
    name = "funktorconf_events",
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks
        .of(emptyList<Repository.Hooks.OnAfterSave<Event>>())
        .plus(timestamped.onBeforeSave())
) {
    companion object {
        suspend fun Storable<Event>.asApiModel() = with(resolve()) {
            EventModel(
                id = _id,
                name = name,
                description = description,
                venue = venue,
                status = status,
                startDate = startDate,
                endDate = endDate,
                createdAt = createdAt.toIsoString(),
                updatedAt = updatedAt.toIsoString(),
            )
        }
    }

    @Suppress("unused")
    class Fixtures(
        repo: EventsRepo,
    ) : RepoFixtureLoader<Event>(repo = repo) {

        val ultraConf = fix {
            "ultra-conf-2026" to Event(
                name = "UltraConf 2026",
                description = "The annual conference for the Ultra library ecosystem. " +
                        "Three days of talks, workshops, and networking covering Kontainer, Kraft, " +
                        "Slumber, Karango, Monko, and the full funktor stack.",
                venue = "Kulturbrauerei, Berlin, Germany",
                status = EventStatus.Published,
                startDate = "2026-06-15",
                endDate = "2026-06-17",
            )
        }

        val funktorSummit = fix {
            "funktor-summit" to Event(
                name = "Funktor Summit 2026",
                description = "A deep-dive into building production web apps with funktor. " +
                        "Hands-on workshops on Kraft UI, Monko repos, typed REST routes, and " +
                        "the kontainer wiring that ties it all together.",
                venue = "Betahaus, Hamburg, Germany",
                status = EventStatus.Draft,
                startDate = "2026-09-20",
                endDate = "2026-09-21",
            )
        }

        val klangHackathon = fix {
            "klang-hackathon" to Event(
                name = "Klang Audio Hackathon",
                description = "A weekend of live coding, sound design, and creative exploration " +
                        "with the Klang Audio Motör. Build instruments, effects, and compositions " +
                        "using KlangScript and the Strudel pattern language.",
                venue = "Ableton Loop Space, Berlin, Germany",
                status = EventStatus.Published,
                startDate = "2026-04-10",
                endDate = "2026-04-11",
            )
        }

        val kraftWorkshop = fix {
            "kraft-workshop" to Event(
                name = "Kraft UI Workshop",
                description = "A single-day intensive workshop on building single-page applications " +
                        "with Kraft and Semantic UI. From components and routing to reactive state with Streams.",
                venue = "Factory Berlin, Germany",
                status = EventStatus.Published,
                startDate = "2026-10-05",
                endDate = "2026-10-05",
            )
        }

        val archivedUltraConf2025 = fix {
            "ultra-conf-2025" to Event(
                name = "UltraConf 2025",
                description = "Last year's UltraConf. Recordings available online. " +
                        "Featured the first public demo of Monko and the funktor inspect tools.",
                venue = "Kulturbrauerei, Berlin, Germany",
                status = EventStatus.Archived,
                startDate = "2025-05-21",
                endDate = "2025-05-23",
            )
        }
    }

    override fun MonkoIndexBuilder<Event>.buildIndexes() {
        persistentIndex {
            field { it.name }
        }

        persistentIndex {
            field { it.status }
        }
    }
}
