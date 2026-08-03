package com.mohnishraj.lunara.data.people

import com.mohnishraj.lunara.domain.ConnectedPerson
import com.mohnishraj.lunara.domain.ConnectionDirection
import com.mohnishraj.lunara.domain.ConnectionRequest
import com.mohnishraj.lunara.domain.PeopleSnapshot
import com.mohnishraj.lunara.domain.UserProfile
import com.mohnishraj.lunara.domain.UserSession
import kotlinx.coroutines.delay
import java.util.UUID

class DemoPeopleRepository : PeopleRepository {
    private val people = listOf(
        profile("10000000-0000-0000-0000-000000000001", "anaya", "Anaya Mehta", "Ideas, photos and unfinished playlists.", 3, "LN-ANAYA24"),
        profile("10000000-0000-0000-0000-000000000002", "kabir_dev", "Kabir Rao", "Building small things with big care.", 1, "LN-KABIR91"),
        profile("10000000-0000-0000-0000-000000000003", "meera", "Meera Kapoor", "Designing calmer digital spaces.", 4, "LN-MEERA08"),
        profile("10000000-0000-0000-0000-000000000004", "arjun_s", "Arjun Shah", "Coffee, code and Sunday football.", 0, "LN-ARJUN73"),
        profile("10000000-0000-0000-0000-000000000005", "zoya", "Zoya Khan", "Collecting stories from ordinary days.", 5, "LN-ZOYA414"),
        profile("10000000-0000-0000-0000-000000000006", "vihaan", "Vihaan Bose", "Music first. Everything else later.", 2, "LN-VIHAN52"),
        profile("10000000-0000-0000-0000-000000000007", "ishita", "Ishita Nair", "Learning, making, sharing.", 3, "LN-ISHI701"),
        profile("10000000-0000-0000-0000-000000000008", "reyansh", "Reyansh Jain", "Always up for a thoughtful conversation.", 1, "LN-REY178"),
    )

    private val incoming = linkedMapOf(
        "demo-in-1" to people[2],
        "demo-in-2" to people[5],
    )
    private val outgoing = linkedMapOf(
        "demo-out-1" to people[4],
    )
    private val connections = linkedMapOf(
        "demo-connected-1" to people[0],
        "demo-connected-2" to people[3],
    )
    private val blocked = linkedMapOf<String, UserProfile>()

    override suspend fun search(session: UserSession, query: String): Result<List<UserProfile>> {
        delay(280)
        val needle = query.trim().removePrefix("@").lowercase()
        val result = if (needle.isBlank()) {
            people
        } else {
            people.filter {
                it.username.lowercase().contains(needle) ||
                    it.displayName.lowercase().contains(needle) ||
                    it.resolvedShareCode.lowercase().contains(needle)
            }
        }.filterNot { blocked.containsKey(it.id) }
        return Result.success(result)
    }

    override suspend fun snapshot(session: UserSession): Result<PeopleSnapshot> {
        delay(240)
        return Result.success(
            PeopleSnapshot(
                incoming = incoming.map { (id, person) -> ConnectionRequest(id, person, ConnectionDirection.Incoming) },
                outgoing = outgoing.map { (id, person) -> ConnectionRequest(id, person, ConnectionDirection.Outgoing) },
                connections = connections.map { (id, person) -> ConnectedPerson(id, person) },
                blocked = blocked.values.toList(),
            )
        )
    }

    override suspend fun sendRequest(session: UserSession, personId: String): Result<Unit> = mutate {
        val person = people.firstOrNull { it.id == personId } ?: error("Profile not found")
        if (!person.allowRequests) error("This person is not accepting requests")
        incoming.entries.removeAll { it.value.id == personId }
        connections.entries.removeAll { it.value.id == personId }
        outgoing[UUID.randomUUID().toString()] = person
    }

    override suspend fun acceptRequest(session: UserSession, requestId: String): Result<Unit> = mutate {
        val person = incoming.remove(requestId) ?: error("Request no longer exists")
        connections[UUID.randomUUID().toString()] = person
    }

    override suspend fun rejectRequest(session: UserSession, requestId: String): Result<Unit> = mutate {
        if (incoming.remove(requestId) == null) error("Request no longer exists")
    }

    override suspend fun cancelRequest(session: UserSession, requestId: String): Result<Unit> = mutate {
        if (outgoing.remove(requestId) == null) error("Request no longer exists")
    }

    override suspend fun removeConnection(session: UserSession, connectionId: String): Result<Unit> = mutate {
        if (connections.remove(connectionId) == null) error("Connection no longer exists")
    }

    override suspend fun block(session: UserSession, personId: String): Result<Unit> = mutate {
        val person = people.firstOrNull { it.id == personId } ?: error("Profile not found")
        incoming.entries.removeAll { it.value.id == personId }
        outgoing.entries.removeAll { it.value.id == personId }
        connections.entries.removeAll { it.value.id == personId }
        blocked[personId] = person
    }

    override suspend fun unblock(session: UserSession, personId: String): Result<Unit> = mutate {
        if (blocked.remove(personId) == null) error("Profile is not blocked")
    }

    override suspend fun updatePrivacy(
        session: UserSession,
        discoverable: Boolean,
        allowRequests: Boolean,
    ): Result<Unit> = mutate { Unit }

    private suspend fun mutate(block: () -> Unit): Result<Unit> {
        delay(260)
        return runCatching(block)
    }

    private fun profile(
        id: String,
        username: String,
        name: String,
        bio: String,
        seed: Int,
        shareCode: String,
    ) = UserProfile(
        id = id,
        email = "$username@preview.lunara",
        username = username,
        displayName = name,
        bio = bio,
        avatarSeed = seed,
        shareCode = shareCode,
    )
}
