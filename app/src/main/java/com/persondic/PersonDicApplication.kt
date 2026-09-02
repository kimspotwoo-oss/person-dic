package com.persondic

import android.app.Application
import androidx.room.Room
import com.persondic.data.local.AppDatabase
import com.persondic.data.repository.PersonDicRepository

class PersonDicApplication : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "persondic.db").build()
    }

    val repository: PersonDicRepository by lazy {
        PersonDicRepository(
            personDao = database.personDao(),
            factDao = database.factDao(),
            interactionDao = database.interactionDao(),
            commitmentDao = database.commitmentDao(),
        )
    }
}
