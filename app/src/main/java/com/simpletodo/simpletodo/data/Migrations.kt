package com.simpletodo.simpletodo.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Added the isImportant flag for marking a schedule as important. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE schedule ADD COLUMN isImportant INTEGER NOT NULL DEFAULT 0")
    }
}

/** Added the recurring_reminder table for weekly repeating reminders. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recurring_reminder` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `daysOfWeekMask` INTEGER NOT NULL,
                `time` INTEGER,
                `isEnabled` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}
