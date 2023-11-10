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

object Migration9to10 : Migration(9, 10) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.apply {
            execSQL(addConditionPeriodColumn)
            execSQL(addConditionExactTimeColumn)
        }
    }

    private val addConditionPeriodColumn = """
        ALTER TABLE `condition_table`
        ADD COLUMN `period` INTEGER
    """.trimIndent()

    private val addConditionExactTimeColumn = """
        ALTER TABLE `condition_table`
        ADD COLUMN `exact_time` TEXT
    """.trimIndent()
}