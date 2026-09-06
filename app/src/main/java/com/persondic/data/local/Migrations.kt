package com.persondic.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `person_group_tag` (" +
                "`personId` TEXT NOT NULL, " +
                "`tag` TEXT NOT NULL, " +
                "PRIMARY KEY(`personId`, `tag`), " +
                "FOREIGN KEY(`personId`) REFERENCES `person`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_person_group_tag_tag` " +
                "ON `person_group_tag` (`tag`)",
        )
        db.execSQL(
            "INSERT OR IGNORE INTO `person_group_tag` (`personId`, `tag`) " +
                "SELECT `id`, TRIM(`groupTag`) FROM `person` " +
                "WHERE `groupTag` IS NOT NULL AND TRIM(`groupTag`) != ''",
        )
    }
}
