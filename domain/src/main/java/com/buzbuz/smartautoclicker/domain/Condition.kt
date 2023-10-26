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
package com.buzbuz.smartautoclicker.domain

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.IntDef

import com.buzbuz.smartautoclicker.database.room.entity.ConditionEntity
import com.buzbuz.smartautoclicker.database.room.entity.ConditionType

/**
 * Condition for a Event.
 */
sealed class Condition {

    /** the unique identifier for the condition. Use 0 for creating a new condition. Default value is 0. */
    abstract var id: Long

    /** the identifier of the event for this condition. */
    abstract var eventId: Long

    /** the name of the condition. */
    abstract val name: String

    /** true if this condition should be detected to be true, false if it should not be found. */
    abstract val shouldBeDetected: Boolean

    /** Cleanup all ids contained in this condition. Ideal for copying. */
    internal fun cleanUpIds() {
        id = 0
        eventId = 0
    }

    /** @return the entity equivalent of this condition. */
    internal abstract fun toEntity(): ConditionEntity

    /** @return creates a deep copy of this condition. */
    abstract fun deepCopy(): Condition

    /**
     * @param path the path to the bitmap that should be matched for detection.
     * @param area  The initial area when the detection condition is created.
     * @param detectArea the area of the screen really to detect.
     * @param threshold the accepted difference between the conditions and the screen content, in percent (0-100%).
     * @param detectionType the type of detection for this condition. Must be one of [DetectionType].
     * @param bitmap the bitmap for the condition. Not set when fetched from the repository.
     * */
    data class Capture(
        override var id: Long,
        override var eventId: Long,
        override val name: String,
        override val shouldBeDetected: Boolean,
        var path: String? = null,
        val area: Rect,
        var detectArea: Rect,
        val threshold: Int,
        @DetectionType val detectionType: Int,
        val bitmap: Bitmap? = null,
    ) : Condition() {

        override fun toEntity(): ConditionEntity = ConditionEntity(
            id = id,
            eventId = eventId,
            name = name,
            type = ConditionType.CAPTURE,
            shouldBeDetected = shouldBeDetected,
            path = path!!,
            areaLeft = area.left,
            areaTop = area.top,
            areaRight = area.right,
            areaBottom = area.bottom,
            detectAreaLeft = detectArea.left,
            detectAreaTop = detectArea.top,
            detectAreaRight = detectArea.right,
            detectAreaBottom = detectArea.bottom,
            threshold = threshold,
            detectionType = detectionType,
        )

        override fun deepCopy(): Capture = copy(
            path = path,
            area = Rect(area),
            detectArea = Rect(detectArea),
        )
    }

    /**
     * @param processName the package name of the process which should be detect as a foreground process
     * */
    data class Process(
        override var id: Long,
        override var eventId: Long,
        override val name: String,
        override val shouldBeDetected: Boolean,
        val processName: String
    ) : Condition() {
        override fun toEntity(): ConditionEntity = ConditionEntity(
            id = id,
            eventId = eventId,
            name = name,
            type = ConditionType.PROCESS,
            shouldBeDetected = shouldBeDetected,
            processName = processName
        )

        override fun deepCopy(): Process = copy(
            processName = processName
        )

    }
}

/** @return the condition for this entity. */
internal fun ConditionEntity.toCondition(): Condition = when (type) {
    ConditionType.CAPTURE -> Condition.Capture(
        id,
        eventId,
        name,
        shouldBeDetected,
        path,
        Rect(areaLeft!!, areaTop!!, areaRight!!, areaBottom!!),
        Rect(detectAreaLeft!!, detectAreaTop!!, detectAreaRight!!, detectAreaBottom!!),
        threshold!!,
        detectionType!!,
    )

    ConditionType.PROCESS -> Condition.Process(
        id,
        eventId,
        name,
        shouldBeDetected,
        processName!!
    )
}


/** Defines the detection type to apply to a condition. */
@IntDef(EXACT, WHOLE_SCREEN, DETECT_AREA)
@Retention(AnnotationRetention.SOURCE)
annotation class DetectionType

/** The condition must be detected at the exact same position. */
const val EXACT = 1

/** The condition can be detected anywhere on the screen. */
const val WHOLE_SCREEN = 2

/** The condition can be detected only in the detection area. */
const val DETECT_AREA = 3
