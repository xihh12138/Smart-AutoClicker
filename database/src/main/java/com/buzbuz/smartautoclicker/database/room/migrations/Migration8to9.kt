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
        ADD COLUMN `detect_area_left` INTEGER DEFAULT 0 NOT NULL
    """.trimIndent()

    /** Create the table for the end conditions. */
    private val addConditionDetectAreaTopColumn = """
        ALTER TABLE `condition_table`
        ADD COLUMN `detect_area_top` INTEGER DEFAULT 0 NOT NULL
    """.trimIndent()

    /** Create the table for the end conditions. */
    private val addConditionDetectAreaRightColumn = """
        ALTER TABLE `condition_table`
        ADD COLUMN `detect_area_right` INTEGER DEFAULT 0 NOT NULL
    """.trimIndent()

    /** Create the table for the end conditions. */
    private val addConditionDetectAreaBottomColumn = """
        ALTER TABLE `condition_table`
        ADD COLUMN `detect_area_bottom` INTEGER DEFAULT 0 NOT NULL
    """.trimIndent()
}