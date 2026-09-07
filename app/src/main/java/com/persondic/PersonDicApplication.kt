package com.persondic

import android.app.Application
import androidx.room.Room
import com.persondic.data.backup.BackupManager
import com.persondic.data.local.AppDatabase
import com.persondic.data.local.MIGRATION_1_2
import com.persondic.data.local.MIGRATION_2_3
import com.persondic.data.local.MIGRATION_3_4
import com.persondic.data.repository.PersonDicRepository
import com.persondic.ui.common.photosDir

class PersonDicApplication : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "persondic.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    val repository: PersonDicRepository by lazy {
        PersonDicRepository(
            database = database,
            personDao = database.personDao(),
            factDao = database.factDao(),
            interactionDao = database.interactionDao(),
            commitmentDao = database.commitmentDao(),
            groupTagDao = database.groupTagDao(),
            tieDao = database.tieDao(),
            personAttributeDao = database.personAttributeDao(),
        )
    }

    val backupManager: BackupManager by lazy { BackupManager(photosDir(this)) }
}
