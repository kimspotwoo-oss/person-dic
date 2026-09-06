package com.persondic.data.repository

import com.persondic.data.local.dao.CommitmentDao
import com.persondic.data.local.dao.FactDao
import com.persondic.data.local.dao.GroupTagDao
import com.persondic.data.local.dao.InteractionDao
import com.persondic.data.local.dao.PersonDao
import com.persondic.data.local.entity.Attendance
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonGroupTag
import com.persondic.data.model.CommitmentStatus
import com.persondic.domain.DerivedValues
import com.persondic.domain.ExpirationCalculator
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class PersonDicRepository(
    private val personDao: PersonDao,
    private val factDao: FactDao,
    private val interactionDao: InteractionDao,
    private val commitmentDao: CommitmentDao,
    private val groupTagDao: GroupTagDao,
) {

    // Person

    fun observePeople(): Flow<List<Person>> = personDao.observeAll()

    fun observePerson(id: UUID): Flow<Person?> = personDao.observeById(id)

    suspend fun addPerson(person: Person) = personDao.insert(person)

    suspend fun updatePerson(person: Person) = personDao.update(person.copy(updatedAt = Instant.now()))

    suspend fun deletePerson(person: Person) = personDao.delete(person)

    suspend fun setPersonPhoto(person: Person, photoUri: String?) {
        personDao.update(person.copy(photoUri = photoUri, updatedAt = Instant.now()))
    }

    // Group tags

    fun observeAllGroupTags(): Flow<List<String>> = groupTagDao.observeAllTags()

    fun observeGroupTags(personId: UUID): Flow<List<String>> = groupTagDao.observeTagsForPerson(personId)

    fun observeAllGroupTagAssignments(): Flow<List<PersonGroupTag>> = groupTagDao.observeAll()

    suspend fun addGroupTag(personId: UUID, tag: String) {
        val normalized = tag.trim()
        if (normalized.isEmpty()) return
        groupTagDao.insert(PersonGroupTag(personId = personId, tag = normalized))
    }

    suspend fun removeGroupTag(personId: UUID, tag: String) = groupTagDao.delete(personId, tag)

    suspend fun findPersonIdsByGroupTag(query: String): List<UUID> =
        groupTagDao.findPersonIdsMatchingTag(query)

    // Fact

    fun observeFacts(personId: UUID): Flow<List<Fact>> = factDao.observeForPerson(personId)

    fun observeFact(id: UUID): Flow<Fact?> = factDao.observeById(id)

    suspend fun findPersonIdsByFactBody(query: String): List<UUID> = factDao.findPersonIdsMatchingBody(query)

    suspend fun addFact(fact: Fact) {
        val withExpiration = fact.copy(
            expiresOn = fact.expiresOn ?: ExpirationCalculator.calculateExpiresOn(fact.volatility, fact.assertedOn),
        )
        factDao.insert(withExpiration)
    }

    suspend fun updateFact(fact: Fact) = factDao.update(fact.copy(updatedAt = Instant.now()))

    suspend fun deleteFact(fact: Fact) = factDao.delete(fact)

    suspend fun renewFactAsStillValid(fact: Fact, today: LocalDate = LocalDate.now()) {
        factDao.update(
            fact.copy(
                assertedOn = today,
                expiresOn = ExpirationCalculator.calculateExpiresOn(fact.volatility, today),
                updatedAt = Instant.now(),
            ),
        )
    }

    // Interaction

    fun observeInteractions(personId: UUID): Flow<List<Interaction>> = interactionDao.observeForPerson(personId)

    suspend fun recordInteraction(interaction: Interaction, personId: UUID) {
        interactionDao.insert(interaction)
        interactionDao.insertAttendance(Attendance(interactionId = interaction.id, personId = personId))
    }

    suspend fun daysSinceLastInteraction(personId: UUID): Long? =
        DerivedValues.daysSinceLastInteraction(interactionDao.lastInteractionAt(personId))

    // Commitment

    fun observeCommitments(personId: UUID): Flow<List<Commitment>> = commitmentDao.observeForPerson(personId)

    fun observeOpenCommitments(personId: UUID): Flow<List<Commitment>> =
        commitmentDao.observeForPersonByStatus(personId, CommitmentStatus.OPEN)

    suspend fun addCommitment(commitment: Commitment) = commitmentDao.insert(commitment)

    suspend fun updateCommitment(commitment: Commitment) = commitmentDao.update(commitment)

    suspend fun deleteCommitment(commitment: Commitment) = commitmentDao.delete(commitment)
}
