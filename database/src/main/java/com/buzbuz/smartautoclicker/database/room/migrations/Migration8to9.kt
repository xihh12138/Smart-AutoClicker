/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.database.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from database v6 to v7.
 *
 * Changes:
 * * creates end condition table
 * * add detection quality to scenario
 * * add end condition operator to scenario
 */
object Migration8to9 : Migration(8, 9) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.apply {
            execSQL(addConditionPriorityColumn)
            execSQL(addConditionDetectAreaLeftColumn)
            execSQL(addConditionDetectAreaTopColumn)
            execSQL(addConditionDetectAreaRightColumn)
            execSQL(addConditionDetectAreaBottomColumn)

            execSQL(renameConditionTableToOld)
            execSQL(createConditionTableNew)
            execSQL(eventToConditionsIndex)
            execSQL(insertConditions)
            execSQL(deleteOldConditionTable)

            execSQL(renameEndConditionTableToOld)
            execSQL(createEndConditionTableNew)
            execSQL(scenarioToEndConditionsIndex)
            execSQL(eventToEndConditionsIndex)
            execSQL(finishEventToEndConditionsIndex)
            execSQL(insertEndConditions)
            execSQL(deleteOldEndConditionTable)
        }
    }

    /** Create the table for the end conditions. */
    private val addConditionPriorityColumn = """
        ALTER TABLE `condition_table`
        ADD COLUMN `priority` INTEGER DEFAULT 0 NOT NULL
    """.trimIndent()

    /** Create the table for the end conditions. */
    private val addConditionDetectAreaLeftColumn = """
        ALTER TABLE `condition_table`
        ADD COLUMN `detect_area_left` INTEGER
    """.trimIndent()

    /** Create the table for the end conditions. */
    private val addConditionDetectAreaTopColumn = """
        ALTER TABLE `condition_table`
        ADD COLUMN `detect_area_top` INTEGER
    """.trimIndent()

    /** Create the table for the end conditions. */
    private val addConditionDetectAreaRightColumn = """
        ALTER TABLE `condition_table`
        ADD COLUMN `detect_area_right` INTEGER
    """.trimIndent()

    /** Create the table for the end conditions. */
    private val addConditionDetectAreaBottomColumn = """
        ALTER TABLE `condition_table`
        ADD COLUMN `detect_area_bottom` INTEGER
    """.trimIndent()

    /** Create the table for the end conditions. */
    private val renameConditionTableToOld = """
        ALTER TABLE `condition_table`
        RENAME TO `condition_table_old`
    """.trimIndent()

    /** Create the table for the end conditions. */
    private val createConditionTableNew = """
        CREATE TABLE IF NOT EXISTS `condition_table` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `eventId` INTEGER NOT NULL,
        `priority` INTEGER NOT NULL DEFAULT 0,
        `name` TEXT NOT NULL,
        `type` TEXT NOT NULL,
        `shouldBeDetected` INTEGER NOT NULL,
        
        `path` TEXT,
        `area_left` INTEGER,
        `area_top` INTEGER,
        `area_right` INTEGER,
        `area_bottom` INTEGER,
        `detect_area_left` INTEGER,
        `detect_area_top` INTEGER,
        `detect_area_right` INTEGER,
        `detect_area_bottom` INTEGER,
        `threshold` INTEGER,
        `detection_type` INTEGER,
        
        `process_name` TEXT,
        FOREIGN KEY(`eventId`) REFERENCES `event_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
    )
    """.trimIndent()

    /** Creates the index between an event and its conditions. */
    private val eventToConditionsIndex = """
        CREATE INDEX IF NOT EXISTS `index_condition_table_eventId` ON `condition_table` (`eventId`)
    """.trimIndent()

    /** Copy the existing conditions into the new table. */
    private val insertConditions = """
        INSERT INTO `condition_table` (eventId, priority, name, type, shouldBeDetected, path, area_left, area_top, area_right, area_bottom, detect_area_left, detect_area_top, detect_area_right, detect_area_bottom, detection_type, threshold)
        SELECT eventId, priority, name, "CAPTURE", shouldBeDetected, path, area_left, area_top, area_right, area_bottom, detect_area_left, detect_area_top, detect_area_right, detect_area_bottom, detection_type, threshold 
        FROM condition_table_old
    """.trimIndent()

    /** Delete the old condition table. */
    private val deleteOldConditionTable = """
        DROP TABLE condition_table_old
    """.trimIndent()

    /** Create the table for the end conditions. */
    private val renameEndConditionTableToOld = """
        ALTER TABLE `end_condition_table`
        RENAME TO `end_condition_table_old`
    """.trimIndent()

    private val createEndConditionTableNew = """
        CREATE TABLE IF NOT EXISTS `end_condition_table` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `scenario_id` INTEGER NOT NULL,
        `event_id` INTEGER NOT NULL,
        `finish_event_id` INTEGER,
        `executions` INTEGER NOT NULL,
        FOREIGN KEY(`scenario_id`) REFERENCES `scenario_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE ,
        FOREIGN KEY(`event_id`) REFERENCES `event_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
        FOREIGN KEY(`finish_event_id`) REFERENCES `event_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
    """.trimIndent()

    private val scenarioToEndConditionsIndex = """
        CREATE INDEX IF NOT EXISTS `index_end_condition_table_scenario_id` ON `end_condition_table` (`scenario_id`)
    """.trimIndent()

    private val eventToEndConditionsIndex = """
        CREATE INDEX IF NOT EXISTS `index_end_condition_table_event_id` ON `end_condition_table` (`event_id`)
    """.trimIndent()

    private val finishEventToEndConditionsIndex = """
        CREATE INDEX IF NOT EXISTS `index_end_condition_table_finish_event_id` ON `end_condition_table` (`finish_event_id`)
    """.trimIndent()

    private val insertEndConditions = """
        INSERT INTO `end_condition_table` (id, scenario_id, event_id, finish_event_id, executions)
        SELECT `id`, `scenario_id`, `event_id`, NULL, `executions` 
        FROM end_condition_table_old
    """.trimIndent()

    private val deleteOldEndConditionTable = """
        DROP TABLE end_condition_table_old
    """.trimIndent()
}