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
package com.buzbuz.smartautoclicker.database.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Entity defining a condition from an event.
 *
 * A condition is composed of a path and size of a bitmap to be matched on the screen, and the position of this matching
 * on the screen.
 *
 * It has a one to many relation with [EventEntity], meaning that one event can have several conditions. If the event is
 * deleted, this condition will be deleted as well.
 *
 * @param id unique identifier for a condition. Also the primary key in the table.
 * @param eventId identifier of this condition's event. Reference the key [EventEntity.id] in event_table.
 * @param name the name of the condition.
 * @param type type of this condition. Must be the this representation of the ConditionType enum.
 * @param shouldBeDetected true if this condition should be detected to be true, false if it should not be found.
 *
 * @param path the path on the application appData directory for the bitmap representing the condition. Also the
 *             primary key for this entity.
 * @param areaLeft the left coordinate of the rectangle defining the matching area.
 * @param areaTop the top coordinate of the rectangle defining the matching area.
 * @param areaRight the right coordinate of the rectangle defining the matching area.
 * @param areaBottom the bottom coordinate of the rectangle defining the matching area.
 * @param threshold the accepted difference between the conditions and the screen content, in percent (0-100%).
 * @param detectionType the type of detection. Can be any of the values defined in
 *                      [com.buzbuz.smartautoclicker.domain.DetectionType].
 *
 *
 * @param processName The package name of the process which should be detect as a foreground process
 *
 * @param period Event interval for triggering conditions in milliseconds。
 */
@Entity(
    tableName = "condition_table",
    indices = [Index("eventId")],
    foreignKeys = [ForeignKey(
        entity = EventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )]
)
@Serializable
data class ConditionEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var id: Long,
    @ColumnInfo(name = "eventId") var eventId: Long,
    @ColumnInfo(name = "priority", defaultValue = "0") var priority: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type", defaultValue = "CAPTURE") val type: ConditionType = ConditionType.CAPTURE,
    @ColumnInfo(name = "shouldBeDetected") val shouldBeDetected: Boolean,

    /** [ConditionType.CAPTURE] */
    @ColumnInfo(name = "path") val path: String? = null,
    @ColumnInfo(name = "area_left") val areaLeft: Int? = null,
    @ColumnInfo(name = "area_top") val areaTop: Int? = null,
    @ColumnInfo(name = "area_right") val areaRight: Int? = null,
    @ColumnInfo(name = "area_bottom") val areaBottom: Int? = null,
    @ColumnInfo(name = "detect_area_left", defaultValue = "0") val detectAreaLeft: Int? = areaLeft,
    @ColumnInfo(name = "detect_area_top", defaultValue = "0") val detectAreaTop: Int? = areaTop,
    @ColumnInfo(name = "detect_area_right", defaultValue = "0") val detectAreaRight: Int? = areaRight,
    @ColumnInfo(name = "detect_area_bottom", defaultValue = "0") val detectAreaBottom: Int? = areaBottom,
    @ColumnInfo(name = "threshold", defaultValue = "1") val threshold: Int? = null,
    @ColumnInfo(name = "detection_type") val detectionType: Int? = null,

    /** [ConditionType.PROCESS] */
    @ColumnInfo(name = "process_name") val processName: String? = null,

    /** [ConditionType.TIMER] */
    @ColumnInfo(name = "period") val period: Long? = null,
    @ColumnInfo(name = "exact_time") val exactTime: String? = null
)

/**
 * Type of [ConditionEntity].
 * For each type there is a set of values that will be available in the database, all others will always be null. Refers
 * to the [ConditionEntity] documentation for values/type association.
 *
 * /!\ DO NOT RENAME: ActionType enum name is used in the database.
 */
enum class ConditionType {
    /** Detect conditions through current screen capture image */
    CAPTURE,

    /**  Detect conditions through current process package name*/
    PROCESS,

    /**  Detect conditions through time */
    TIMER,
}

fun Iterable<ConditionEntity>.sortByPriority() = sortedWith { o1, o2 ->
    val comp = o1.priority.compareTo(o2.priority)
    if (comp == 0) {
        o1.id.compareTo(o2.id)
    } else {
        comp
    }
}