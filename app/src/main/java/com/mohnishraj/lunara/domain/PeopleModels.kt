package com.mohnishraj.lunara.domain

enum class ConnectionDirection {
    Incoming,
    Outgoing,
}

data class ConnectionRequest(
    val id: String,
    val person: UserProfile,
    val direction: ConnectionDirection,
    val createdAt: String = "",
)

data class ConnectedPerson(
    val connectionId: String,
    val person: UserProfile,
    val connectedAt: String = "",
)

data class PeopleSnapshot(
    val incoming: List<ConnectionRequest> = emptyList(),
    val outgoing: List<ConnectionRequest> = emptyList(),
    val connections: List<ConnectedPerson> = emptyList(),
    val blocked: List<UserProfile> = emptyList(),
)

enum class RelationshipState {
    None,
    IncomingRequest,
    OutgoingRequest,
    Connected,
    Blocked,
}

fun PeopleSnapshot.relationshipFor(personId: String): RelationshipState = when {
    blocked.any { it.id == personId } -> RelationshipState.Blocked
    connections.any { it.person.id == personId } -> RelationshipState.Connected
    incoming.any { it.person.id == personId } -> RelationshipState.IncomingRequest
    outgoing.any { it.person.id == personId } -> RelationshipState.OutgoingRequest
    else -> RelationshipState.None
}
