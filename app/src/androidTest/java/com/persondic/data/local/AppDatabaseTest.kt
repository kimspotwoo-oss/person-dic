package com.persondic.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.persondic.data.local.entity.Attendance
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.local.entity.Person
import com.persondic.data.model.CommitmentStatus
import com.persondic.data.model.Direction
import com.persondic.data.model.FactCategory
import com.persondic.data.model.InteractionKind
import com.persondic.data.model.Sensitivity
import com.persondic.data.model.Volatility
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndReadPerson() = runTest {
        val person = Person(displayName = "김민준", groupTag = "대학동창")
        db.personDao().insert(person)

        val loaded = db.personDao().observeById(person.id).first()

        assertEquals(person.displayName, loaded?.displayName)
    }

    @Test
    fun deletingPersonCascadesToFacts() = runTest {
        val person = Person(displayName = "이서연")
        db.personDao().insert(person)
        db.factDao().insert(
            Fact(
                personId = person.id,
                category = FactCategory.LIFE,
                body = "갑각류 알레르기",
                volatility = Volatility.PERMANENT,
                assertedOn = LocalDate.of(2026, 1, 1),
                sensitivity = Sensitivity.NORMAL,
            ),
        )

        assertEquals(1, db.factDao().observeForPerson(person.id).first().size)

        db.personDao().delete(person)

        assertTrue(db.factDao().observeForPerson(person.id).first().isEmpty())
    }

    @Test
    fun interactionJoinsThroughAttendance() = runTest {
        val person = Person(displayName = "박지훈")
        db.personDao().insert(person)

        val interaction = Interaction(
            metAt = Instant.parse("2026-08-01T09:00:00Z"),
            place = "카페",
            summary = "오랜만에 근황 토크",
            kind = InteractionKind.MEET,
        )
        db.interactionDao().insert(interaction)
        db.interactionDao().insertAttendance(Attendance(interactionId = interaction.id, personId = person.id))

        val forPerson = db.interactionDao().observeForPerson(person.id).first()

        assertEquals(1, forPerson.size)
        assertEquals(interaction.id, forPerson.first().id)
        assertEquals(interaction.metAt, db.interactionDao().lastInteractionAt(person.id))
    }

    @Test
    fun commitmentFiltersByStatus() = runTest {
        val person = Person(displayName = "최유나")
        db.personDao().insert(person)
        db.commitmentDao().insert(
            Commitment(personId = person.id, direction = Direction.I_OWE, body = "책 빌려주기", status = CommitmentStatus.OPEN),
        )
        db.commitmentDao().insert(
            Commitment(personId = person.id, direction = Direction.THEY_OWE, body = "이직 결과 물어보기", status = CommitmentStatus.DONE),
        )

        val open = db.commitmentDao().observeForPersonByStatus(person.id, CommitmentStatus.OPEN).first()

        assertEquals(1, open.size)
        assertEquals("책 빌려주기", open.first().body)
    }

    @Test
    fun factBodySearchFindsThePerson() = runTest {
        val allergic = Person(displayName = "정하은")
        val other = Person(displayName = "오세훈")
        db.personDao().insert(allergic)
        db.personDao().insert(other)
        db.factDao().insert(
            Fact(
                personId = allergic.id,
                category = FactCategory.LIFE,
                body = "갑각류 알레르기 있음",
                volatility = Volatility.PERMANENT,
                assertedOn = LocalDate.of(2026, 1, 1),
                sensitivity = Sensitivity.NORMAL,
            ),
        )
        db.factDao().insert(
            Fact(
                personId = other.id,
                category = FactCategory.PREFERENCE,
                body = "커피보다 차를 좋아함",
                volatility = Volatility.SLOW,
                assertedOn = LocalDate.of(2026, 1, 1),
                sensitivity = Sensitivity.NORMAL,
            ),
        )

        val matches = db.factDao().findPersonIdsMatchingBody("알레르기")

        assertEquals(listOf(allergic.id), matches)
    }
}
