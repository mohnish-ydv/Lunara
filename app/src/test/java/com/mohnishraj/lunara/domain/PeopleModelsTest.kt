package com.mohnishraj.lunara.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PeopleModelsTest {
    private val person = UserProfile(
        id = "person-1",
        email = "person@example.com",
        username = "person",
        displayName = "Person",
    )

    @Test
    fun blockedRelationshipHasHighestPriority() {
        val snapshot = PeopleSnapshot(
            incoming = listOf(ConnectionRequest("request", person, ConnectionDirection.Incoming)),
            blocked = listOf(person),
        )

        assertEquals(RelationshipState.Blocked, snapshot.relationshipFor(person.id))
    }

    @Test
    fun acceptedConnectionIsDetected() {
        val snapshot = PeopleSnapshot(
            connections = listOf(ConnectedPerson("connection", person)),
        )

        assertEquals(RelationshipState.Connected, snapshot.relationshipFor(person.id))
    }

    @Test
    fun unknownProfileHasNoRelationship() {
        assertEquals(RelationshipState.None, PeopleSnapshot().relationshipFor(person.id))
    }
}
