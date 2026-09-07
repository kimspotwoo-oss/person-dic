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

/**
 * Fixed information: a birthday on Person plus a free-form attribute table.
 *
 * The birthday columns go on Person rather than into Fact because a birthday never goes stale —
 * in Fact it would get an expiry date and start asking to be re-confirmed.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `person` ADD COLUMN `birthday` INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE `person` ADD COLUMN `birthdayHasYear` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `person` ADD COLUMN `birthdayIsLunar` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `person_attribute` (" +
                "`personId` TEXT NOT NULL, " +
                "`label` TEXT NOT NULL, " +
                "`value` TEXT NOT NULL, " +
                "`sortOrder` INTEGER NOT NULL, " +
                "PRIMARY KEY(`personId`, `label`), " +
                "FOREIGN KEY(`personId`) REFERENCES `person`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_person_attribute_personId` " +
                "ON `person_attribute` (`personId`)",
        )
    }
}

/** Keeps the conversation itself, not only the one-line summary of it. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `interaction` ADD COLUMN `notes` TEXT DEFAULT NULL")
    }
}
