package io.peekandpoke.funktor.demo.server.funktorconf

class FunktorConfServices(
    eventsRepo: Lazy<EventsRepo>,
    speakersRepo: Lazy<SpeakersRepo>,
    attendeesRepo: Lazy<AttendeesRepo>,
) {
    val eventsRepo by eventsRepo
    val speakersRepo by speakersRepo
    val attendeesRepo by attendeesRepo
}
