package com.persondic.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.persondic.data.local.entity.BIRTHDAY_YEAR_UNKNOWN

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

/**
 * Splits 몇년생 out of the birthday, and gives each fixed-information entry its own sensitivity.
 *
 * The year moves from inside `birthday` into its own column and `birthday` is rewritten to the
 * stand-in year, so there is exactly one place the year can be read from. `birthdayHasYear` is
 * left behind deliberately: see Person.birthdayHasYear for why it cannot be dropped.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `person` ADD COLUMN `birthYear` INTEGER DEFAULT NULL")

        // Dates are stored as epoch days, so *86400 turns them into the epoch seconds
        // strftime understands.
        db.execSQL(
            "UPDATE `person` SET `birthYear` = " +
                "CAST(strftime('%Y', `birthday` * 86400, 'unixepoch') AS INTEGER) " +
                "WHERE `birthdayHasYear` = 1 AND `birthday` IS NOT NULL",
        )
        db.execSQL(
            "UPDATE `person` SET `birthday` = CAST(" +
                "julianday('$BIRTHDAY_YEAR_UNKNOWN-' || strftime('%m-%d', `birthday` * 86400, 'unixepoch')) " +
                "- julianday('1970-01-01') AS INTEGER) " +
                "WHERE `birthday` IS NOT NULL",
        )

        db.execSQL(
            "ALTER TABLE `person_attribute` ADD COLUMN `sensitivity` TEXT NOT NULL DEFAULT 'NORMAL'",
        )
    }
}

/**
 * Adds the owner's own row and marks which ties read the same from both ends.
 *
 * Existing ties are backfilled from the labels the app suggested up to now, so a 배우자 recorded
 * before this migration does not start rendering as a one-way arrow.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `person` ADD COLUMN `isSelf` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `tie` ADD COLUMN `symmetric` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "UPDATE `tie` SET `symmetric` = 1 WHERE `label` IN " +
                "('친구', '배우자', '형제자매', '직장 동료')",
        )
    }
}
