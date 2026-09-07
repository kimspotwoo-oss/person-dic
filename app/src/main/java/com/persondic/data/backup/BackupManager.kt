package com.persondic.data.backup

import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class ImportFailure {
    /** The archive had no backup.json, so it is some other zip file. */
    NOT_A_BACKUP,

    /** Written by a newer build whose format this one cannot read. */
    UNSUPPORTED_VERSION,

    /** Unreadable stream or corrupt archive. */
    UNREADABLE,
}

data class ExportSummary(
    val people: Int,
    val facts: Int,
    val photos: Int,
)

sealed interface ImportResult {
    data class Success(
        val snapshot: BackupSnapshot,
        val skippedRows: Int,
        val photos: Int,
    ) : ImportResult

    data class Failure(val reason: ImportFailure) : ImportResult
}

/**
 * Reads and writes the backup archive: a zip holding [BACKUP_JSON_NAME] plus the person photos.
 *
 * Photos travel inside the archive because [com.persondic.data.local.entity.Person.photoUri] is an
 * absolute device path — a JSON-only backup would restore onto paths that no longer exist. On the
 * way in, photos land under fresh names in [photosDir] and the restored rows point at those.
 *
 * Takes streams rather than files so the caller can hand it a Storage Access Framework document
 * and the app never needs a storage permission.
 */
class BackupManager(
    private val photosDir: File,
    private val now: () -> Instant = Instant::now,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun export(snapshot: BackupSnapshot, out: OutputStream): ExportSummary {
        var photoCount = 0
        ZipOutputStream(out.buffered()).use { zip ->
            val people = snapshot.people.map { person ->
                val photo = person.photoUri?.let { File(it) }?.takeIf { it.isFile }
                if (photo == null) {
                    person.toBackup(photoName = null)
                } else {
                    val name = "${person.id}.${photo.extension.ifEmpty { "jpg" }}"
                    zip.putNextEntry(ZipEntry("$BACKUP_PHOTO_DIR/$name"))
                    photo.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    photoCount++
                    person.toBackup(photoName = name)
                }
            }

            val file = BackupFile(
                formatVersion = BACKUP_FORMAT_VERSION,
                exportedAt = now().toString(),
                people = people,
                facts = snapshot.facts.map { it.toBackup() },
                interactions = snapshot.interactions.map { it.toBackup() },
                attendances = snapshot.attendances.map { it.toBackup() },
                commitments = snapshot.commitments.map { it.toBackup() },
                groupTags = snapshot.groupTags.map { it.toBackup() },
                ties = snapshot.ties.map { it.toBackup() },
            )

            zip.putNextEntry(ZipEntry(BACKUP_JSON_NAME))
            zip.write(json.encodeToString(BackupFile.serializer(), file).toByteArray())
            zip.closeEntry()
        }
        return ExportSummary(
            people = snapshot.people.size,
            facts = snapshot.facts.size,
            photos = photoCount,
        )
    }

    fun import(input: InputStream): ImportResult {
        // Photos are unpacked to a staging folder first: if the json turns out to be unreadable,
        // nothing has been added to the real photo folder yet and the staging folder is dropped.
        val staging = File(photosDir, STAGING_DIR)
        return try {
            staging.deleteRecursively()
            staging.mkdirs()

            var jsonBytes: ByteArray? = null
            val staged = mutableMapOf<String, File>()

            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        !entry.isDirectory && name == BACKUP_JSON_NAME -> jsonBytes = zip.readBytes()

                        !entry.isDirectory && isSafePhotoEntry(name) -> {
                            val target = File(staging, "${UUID.randomUUID()}.${File(name).extension.ifEmpty { "jpg" }}")
                            target.outputStream().use { zip.copyTo(it) }
                            staged[name.removePrefix("$BACKUP_PHOTO_DIR/")] = target
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val bytes = jsonBytes ?: return failed(staging, ImportFailure.NOT_A_BACKUP)
            val parsed = runCatching { json.decodeFromString(BackupFile.serializer(), bytes.decodeToString()) }
                .getOrNull() ?: return failed(staging, ImportFailure.NOT_A_BACKUP)
            if (parsed.formatVersion > BACKUP_FORMAT_VERSION) {
                return failed(staging, ImportFailure.UNSUPPORTED_VERSION)
            }

            var skipped = 0
            fun <T, R> List<T>.mapKept(transform: (T) -> R?): List<R> =
                mapNotNull { transform(it).also { mapped -> if (mapped == null) skipped++ } }

            photosDir.mkdirs()
            val restoredPhotos = mutableMapOf<String, String>()
            staged.forEach { (entryName, file) ->
                val target = File(photosDir, file.name)
                if (file.renameTo(target) || copyInto(file, target)) {
                    restoredPhotos[entryName] = target.absolutePath
                }
            }

            val snapshot = BackupSnapshot(
                people = parsed.people.mapKept { it.toEntity(photoPath = restoredPhotos[it.photo]) },
                facts = parsed.facts.mapKept { it.toEntity() },
                interactions = parsed.interactions.mapKept { it.toEntity() },
                attendances = parsed.attendances.mapKept { it.toEntity() },
                commitments = parsed.commitments.mapKept { it.toEntity() },
                groupTags = parsed.groupTags.mapKept { it.toEntity() },
                ties = parsed.ties.mapKept { it.toEntity() },
            )

            staging.deleteRecursively()
            ImportResult.Success(snapshot = snapshot, skippedRows = skipped, photos = restoredPhotos.size)
        } catch (e: Exception) {
            staging.deleteRecursively()
            ImportResult.Failure(ImportFailure.UNREADABLE)
        }
    }

    private fun failed(staging: File, reason: ImportFailure): ImportResult {
        staging.deleteRecursively()
        return ImportResult.Failure(reason)
    }

    private fun copyInto(source: File, target: File): Boolean =
        runCatching { source.copyTo(target, overwrite = true) }.isSuccess

    /**
     * Zip entry names are attacker-controlled in the sense that the file came from outside the app,
     * so only plain names directly inside photos/ are unpacked. Anything with a path separator or a
     * parent reference left in it is ignored instead of being written somewhere unexpected.
     */
    private fun isSafePhotoEntry(name: String): Boolean {
        if (!name.startsWith("$BACKUP_PHOTO_DIR/")) return false
        val leaf = name.removePrefix("$BACKUP_PHOTO_DIR/")
        return leaf.isNotEmpty() && !leaf.contains('/') && !leaf.contains('\\') && leaf != ".." && leaf != "."
    }

    private companion object {
        const val STAGING_DIR = ".import-staging"
    }
}
