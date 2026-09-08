package com.persondic.data.backup

import com.persondic.data.local.entity.Attendance
import com.persondic.data.local.entity.BIRTHDAY_YEAR_UNKNOWN
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonAttribute
import com.persondic.data.local.entity.PersonGroupTag
import com.persondic.data.local.entity.Tie
import com.persondic.data.model.CommitmentStatus
import com.persondic.data.model.Direction
import com.persondic.data.model.FactCategory
import com.persondic.data.model.InteractionKind
import com.persondic.data.model.Sensitivity
import com.persondic.data.model.Volatility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManagerTest {

    private lateinit var root: File
    private lateinit var photosDir: File
    private lateinit var manager: BackupManager

    private val personId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val otherId = UUID.fromString("22222222-2222-2222-2222-222222222222")
    private val interactionId = UUID.fromString("33333333-3333-3333-3333-333333333333")

    @Before
    fun setUp() {
        root = File(System.getProperty("java.io.tmpdir"), "persondic-test-${UUID.randomUUID()}")
        photosDir = File(root, "photos").apply { mkdirs() }
        manager = BackupManager(photosDir) { Instant.parse("2026-09-07T12:00:00Z") }
    }

    /** Every field carries a distinct non-default value, so a field the mapper forgets fails here. */
    private fun fullSnapshot(photoPath: String? = null) = BackupSnapshot(
        people = listOf(
            Person(
                id = personId,
                displayName = "김민준",
                alias = "민준이",
                groupTag = "대학동창",
                metOn = LocalDate.parse("2019-03-02"),
                metStory = "동아리에서 처음 만남",
                photoUri = photoPath,
                birthday = LocalDate.of(BIRTHDAY_YEAR_UNKNOWN, 2, 29),
                birthYear = 1988,
                birthdayIsLunar = true,
                birthdayHasYear = true,
                createdAt = Instant.parse("2026-01-01T01:02:03Z"),
                updatedAt = Instant.parse("2026-02-02T04:05:06Z"),
            ),
            Person(id = otherId, displayName = "이서연", isSelf = true),
        ),
        facts = listOf(
            Fact(
                id = UUID.fromString("44444444-4444-4444-4444-444444444444"),
                personId = personId,
                category = FactCategory.PREFERENCE,
                body = "갑각류 알레르기",
                volatility = Volatility.PERMANENT,
                assertedOn = LocalDate.parse("2026-03-04"),
                expiresOn = LocalDate.parse("2028-03-04"),
                confidence = 0.5f,
                sensitivity = Sensitivity.RESTRICTED,
                pinned = true,
                sourceId = UUID.fromString("55555555-5555-5555-5555-555555555555"),
                supersededBy = UUID.fromString("66666666-6666-6666-6666-666666666666"),
                createdAt = Instant.parse("2026-03-04T07:08:09Z"),
                updatedAt = Instant.parse("2026-03-05T10:11:12Z"),
            ),
        ),
        interactions = listOf(
            Interaction(
                id = interactionId,
                metAt = Instant.parse("2026-04-05T13:14:15Z"),
                place = "학교 앞 카페",
                summary = "이직 얘기",
                notes = "회사 얘기 한참 하다가\n등산 얘기로 넘어감",
                kind = InteractionKind.MESSAGE,
            ),
        ),
        attendances = listOf(Attendance(interactionId = interactionId, personId = personId)),
        commitments = listOf(
            Commitment(
                id = UUID.fromString("77777777-7777-7777-7777-777777777777"),
                personId = personId,
                direction = Direction.THEY_OWE,
                body = "책 돌려주기",
                dueOn = LocalDate.parse("2026-05-06"),
                status = CommitmentStatus.DONE,
                createdAt = Instant.parse("2026-04-06T16:17:18Z"),
            ),
        ),
        groupTags = listOf(
            PersonGroupTag(personId = personId, tag = "대학동창"),
            PersonGroupTag(personId = personId, tag = "등산"),
        ),
        attributes = listOf(
            PersonAttribute(personId = personId, label = "혈액형", value = "A", sortOrder = 1),
            PersonAttribute(
                personId = personId,
                label = "종교",
                value = "천주교",
                sensitivity = Sensitivity.PRIVATE,
                sortOrder = 2,
            ),
        ),
        ties = listOf(
            Tie(
                id = UUID.fromString("88888888-8888-8888-8888-888888888888"),
                fromPersonId = personId,
                toPersonId = otherId,
                label = "직장 동료",
                symmetric = true,
            ),
        ),
    )

    private fun roundTrip(snapshot: BackupSnapshot): ImportResult {
        val bytes = ByteArrayOutputStream().also { manager.export(snapshot, it) }.toByteArray()
        return manager.import(ByteArrayInputStream(bytes))
    }

    @Test
    fun roundTripsEveryFieldOfEveryTable() {
        val original = fullSnapshot()
        val result = roundTrip(original) as ImportResult.Success

        assertEquals(0, result.skippedRows)
        assertEquals(original.people, result.snapshot.people)
        assertEquals(original.facts, result.snapshot.facts)
        assertEquals(original.interactions, result.snapshot.interactions)
        assertEquals(original.attendances, result.snapshot.attendances)
        assertEquals(original.commitments, result.snapshot.commitments)
        assertEquals(original.groupTags, result.snapshot.groupTags)
        assertEquals(original.ties, result.snapshot.ties)
        assertEquals(original.attributes, result.snapshot.attributes)
    }

    @Test
    fun exportSummaryCountsWhatWasWritten() {
        val summary = ByteArrayOutputStream().let { manager.export(fullSnapshot(), it) }

        assertEquals(2, summary.people)
        assertEquals(1, summary.facts)
        assertEquals(0, summary.photos)
    }

    @Test
    fun photosTravelInsideTheArchiveAndComeBackAsUsableFiles() {
        val source = File(root, "original.jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5)) }
        val snapshot = fullSnapshot(photoPath = source.absolutePath)

        val bytes = ByteArrayOutputStream().also { manager.export(snapshot, it) }.toByteArray()
        source.delete()

        val result = manager.import(ByteArrayInputStream(bytes)) as ImportResult.Success
        assertEquals(1, result.photos)

        val restoredPath = result.snapshot.people.first { it.id == personId }.photoUri
        assertNotNull(restoredPath)
        val restored = File(requireNotNull(restoredPath))
        assertTrue("restored photo should exist", restored.exists())
        assertEquals(listOf<Byte>(1, 2, 3, 4, 5), restored.readBytes().toList())
        assertEquals(photosDir.absolutePath, restored.parentFile?.absolutePath)
        assertNotEquals(source.absolutePath, restoredPath)
    }

    @Test
    fun aPersonWhoseFileVanishedBeforeExportRestoresWithoutAPhoto() {
        val snapshot = fullSnapshot(photoPath = File(root, "gone.jpg").absolutePath)

        val result = roundTrip(snapshot) as ImportResult.Success

        assertEquals(0, result.photos)
        assertNull(result.snapshot.people.first { it.id == personId }.photoUri)
    }

    @Test
    fun emptyDatabaseRoundTrips() {
        val result = roundTrip(BackupSnapshot()) as ImportResult.Success

        assertTrue(result.snapshot.isEmpty)
        assertEquals(0, result.skippedRows)
    }

    @Test
    fun importingTheSameFileTwiceGivesTheSameResult() {
        val bytes = ByteArrayOutputStream().also { manager.export(fullSnapshot(), it) }.toByteArray()

        val first = manager.import(ByteArrayInputStream(bytes)) as ImportResult.Success
        val second = manager.import(ByteArrayInputStream(bytes)) as ImportResult.Success

        assertEquals(first.snapshot, second.snapshot)
    }

    @Test
    fun aZipWithoutBackupJsonIsRejected() {
        val bytes = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("something-else.txt"))
                zip.write("hello".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()

        val result = manager.import(ByteArrayInputStream(bytes))

        assertEquals(ImportFailure.NOT_A_BACKUP, (result as ImportResult.Failure).reason)
    }

    @Test
    fun somethingThatIsNotAZipIsRejectedWithoutCrashing() {
        val result = manager.import(ByteArrayInputStream("not a zip at all".toByteArray()))

        assertTrue(result is ImportResult.Failure)
    }

    @Test
    fun aNewerFormatVersionIsRefusedRatherThanPartlyRead() {
        val bytes = zipOf(
            BACKUP_JSON_NAME to """{"formatVersion":${BACKUP_FORMAT_VERSION + 1},"people":[]}""",
        )

        val result = manager.import(ByteArrayInputStream(bytes))

        assertEquals(ImportFailure.UNSUPPORTED_VERSION, (result as ImportResult.Failure).reason)
    }

    @Test
    fun unreadableRowsAreSkippedAndCountedInsteadOfFailingTheWholeImport() {
        val bytes = zipOf(
            BACKUP_JSON_NAME to """
            {
              "formatVersion": 1,
              "people": [
                {"id": "not-a-uuid", "displayName": "깨진 사람"},
                {"id": "$personId", "displayName": "   "},
                {"id": "$otherId", "displayName": "정상"}
              ]
            }
            """.trimIndent(),
        )

        val result = manager.import(ByteArrayInputStream(bytes)) as ImportResult.Success

        assertEquals(1, result.snapshot.people.size)
        assertEquals("정상", result.snapshot.people.single().displayName)
        assertEquals(2, result.skippedRows)
    }

    @Test
    fun anUnknownEnumNameFallsBackInsteadOfLosingTheFact() {
        val bytes = zipOf(
            BACKUP_JSON_NAME to """
            {
              "formatVersion": 1,
              "facts": [
                {
                  "id": "$interactionId", "personId": "$personId", "category": "FUTURE_CATEGORY",
                  "body": "새 빌드에서 온 사실", "volatility": "???", "assertedOn": "2026-03-04",
                  "sensitivity": "NOPE"
                }
              ]
            }
            """.trimIndent(),
        )

        val result = manager.import(ByteArrayInputStream(bytes)) as ImportResult.Success

        val fact = result.snapshot.facts.single()
        assertEquals("새 빌드에서 온 사실", fact.body)
        assertEquals(FactCategory.LIFE, fact.category)
        assertEquals(Volatility.SEASONAL, fact.volatility)
        assertEquals(Sensitivity.NORMAL, fact.sensitivity)
        assertEquals(0, result.skippedRows)
    }

    @Test
    fun aFormatOneFileStillReadsWithTheYearInsideTheBirthday() {
        val bytes = zipOf(
            BACKUP_JSON_NAME to """
            {
              "formatVersion": 1,
              "people": [
                {"id": "$personId", "displayName": "김민준",
                 "birthday": "1990-03-15", "birthdayHasYear": true},
                {"id": "$otherId", "displayName": "이서연",
                 "birthday": "2000-07-04", "birthdayHasYear": false}
              ]
            }
            """.trimIndent(),
        )

        val result = manager.import(ByteArrayInputStream(bytes)) as ImportResult.Success

        val known = result.snapshot.people.first { it.id == personId }
        assertEquals(1990, known.birthYear)
        assertEquals(LocalDate.of(BIRTHDAY_YEAR_UNKNOWN, 3, 15), known.birthday)

        // birthdayHasYear was false, so 2000 was the stand-in year, not a real one.
        val unknownYear = result.snapshot.people.first { it.id == otherId }
        assertNull(unknownYear.birthYear)
        assertEquals(LocalDate.of(BIRTHDAY_YEAR_UNKNOWN, 7, 4), unknownYear.birthday)
    }

    @Test
    fun unknownJsonKeysFromANewerBuildAreIgnored() {
        val bytes = zipOf(
            BACKUP_JSON_NAME to """
            {
              "formatVersion": 1,
              "somethingAddedLater": {"a": 1},
              "people": [{"id": "$personId", "displayName": "김민준", "nickname": "새 필드"}]
            }
            """.trimIndent(),
        )

        val result = manager.import(ByteArrayInputStream(bytes)) as ImportResult.Success

        assertEquals("김민준", result.snapshot.people.single().displayName)
    }

    @Test
    fun photoEntriesThatTryToEscapeThePhotoFolderAreIgnored() {
        val bytes = zipOf(
            "photos/../../evil.jpg" to "pwned",
            "../evil2.jpg" to "pwned",
            BACKUP_JSON_NAME to """{"formatVersion":1,"people":[]}""",
        )

        val result = manager.import(ByteArrayInputStream(bytes)) as ImportResult.Success

        assertEquals(0, result.photos)
        assertTrue(File(root, "evil.jpg").let { !it.exists() })
        assertTrue(File(root.parentFile, "evil2.jpg").let { !it.exists() })
    }

    @Test
    fun aFailedImportLeavesNoStagedPhotosBehind() {
        val bytes = zipOf("photos/a.jpg" to "bytes", "readme.txt" to "no json here")

        assertTrue(manager.import(ByteArrayInputStream(bytes)) is ImportResult.Failure)
        assertEquals(emptyList<String>(), photosDir.listFiles().orEmpty().map { it.name })
    }

    @Test
    fun theArchiveIsAPlainZipHoldingBackupJson() {
        val bytes = ByteArrayOutputStream().also { manager.export(fullSnapshot(), it) }.toByteArray()

        val names = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                names += entry.name
                entry = zip.nextEntry
            }
        }

        assertTrue(BACKUP_JSON_NAME in names)
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray =
        ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                entries.forEach { (name, body) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(body.toByteArray())
                    zip.closeEntry()
                }
            }
        }.toByteArray()
}
