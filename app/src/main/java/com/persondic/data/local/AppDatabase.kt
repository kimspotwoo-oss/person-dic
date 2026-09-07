package com.persondic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.persondic.data.local.dao.CommitmentDao
import com.persondic.data.local.dao.FactDao
import com.persondic.data.local.dao.GroupTagDao
import com.persondic.data.local.dao.InteractionDao
import com.persondic.data.local.dao.PersonAttributeDao
import com.persondic.data.local.dao.PersonDao
import com.persondic.data.local.dao.TieDao
import com.persondic.data.local.entity.Attendance
import com.persondic.data.local.entity.Commitment
import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Interaction
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonAttribute
import com.persondic.data.local.entity.PersonGroupTag
import com.persondic.data.local.entity.Tie

@Database(
    entities = [
        Person::class,
        Fact::class,
        Interaction::class,
        Attendance::class,
        Commitment::class,
        Tie::class,
        PersonGroupTag::class,
        PersonAttribute::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun factDao(): FactDao
    abstract fun interactionDao(): InteractionDao
    abstract fun commitmentDao(): CommitmentDao
    abstract fun groupTagDao(): GroupTagDao
    abstract fun tieDao(): TieDao
    abstract fun personAttributeDao(): PersonAttributeDao
}
