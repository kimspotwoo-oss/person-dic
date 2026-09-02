package com.persondic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.persondic.data.local.dao.CommitmentDao
import com.persondic.data.local.dao.FactDao
import com.persondic.data.local.dao.InteractionDao
import com.persondic.data.local.dao.PersonDao
import com.persondic.data.local.entity.Attendance
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.Tie

@Database(
    entities = [
        Person::class,
        Fact::class,
        Interaction::class,
        Attendance::class,
        Commitment::class,
        Tie::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun factDao(): FactDao
    abstract fun interactionDao(): InteractionDao
    abstract fun commitmentDao(): CommitmentDao
}
