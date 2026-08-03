package com.mohnishraj.lunara.data.people

import com.mohnishraj.lunara.domain.RelationshipState
import com.mohnishraj.lunara.domain.UserSession
import com.mohnishraj.lunara.domain.relationshipFor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoPeopleRepositoryTest {
    private val session = UserSession("token", "refresh", "current-user", "me@example.com")

    @Test
    fun requestLifecycleUpdatesSnapshot() = runBlocking {
        val repository = DemoPeopleRepository()
        val person = repository.search(session, "ishita").getOrThrow().single()

        repository.sendRequest(session, person.id).getOrThrow()
        val requested = repository.snapshot(session).getOrThrow()
        assertEquals(RelationshipState.OutgoingRequest, requested.relationshipFor(person.id))

        val requestId = requested.outgoing.single { it.person.id == person.id }.id
        repository.cancelRequest(session, requestId).getOrThrow()
        assertEquals(RelationshipState.None, repository.snapshot(session).getOrThrow().relationshipFor(person.id))
    }

    @Test
    fun blockingRemovesExistingRelationship() = runBlocking {
        val repository = DemoPeopleRepository()
        val connected = repository.snapshot(session).getOrThrow().connections.first().person

        repository.block(session, connected.id).getOrThrow()
        val snapshot = repository.snapshot(session).getOrThrow()

        assertEquals(RelationshipState.Blocked, snapshot.relationshipFor(connected.id))
        assertTrue(snapshot.connections.none { it.person.id == connected.id })
    }
}
