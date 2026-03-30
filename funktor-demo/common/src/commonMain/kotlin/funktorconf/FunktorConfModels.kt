package io.peekandpoke.funktor.demo.common.funktorconf

import kotlinx.serialization.Serializable

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  Event
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@Serializable
data class EventModel(
    val id: String,
    val name: String,
    val description: String,
    val venue: String,
    val status: EventStatus,
    val startDate: String,
    val endDate: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
enum class EventStatus {
    Draft,
    Published,
    Archived,
}

@Serializable
data class SaveEventRequest(
    val name: String,
    val description: String,
    val venue: String,
    val status: EventStatus,
    val startDate: String,
    val endDate: String,
)

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  Speaker
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@Serializable
data class SpeakerModel(
    val id: String,
    val name: String,
    val bio: String,
    val photoUrl: String,
    val talkTitle: String,
    val talkAbstract: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class SaveSpeakerRequest(
    val name: String,
    val bio: String,
    val photoUrl: String,
    val talkTitle: String,
    val talkAbstract: String,
)

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  Attendee
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@Serializable
data class AttendeeModel(
    val id: String,
    val name: String,
    val email: String,
    val ticketType: TicketType,
    val checkedIn: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
enum class TicketType {
    Standard,
    Vip,
    Speaker,
    Staff,
}

@Serializable
data class SaveAttendeeRequest(
    val name: String,
    val email: String,
    val ticketType: TicketType,
    val checkedIn: Boolean,
)
