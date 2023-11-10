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
package com.buzbuz.smartautoclicker.overlays.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.AND
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.EXACT
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.OR
import com.buzbuz.smartautoclicker.domain.Scenario

/**
 * Creates a new event.
 *
 * @param context the Android context.
 * @return the new event.
 */
fun newDefaultScenario(context: Context, scenarioName: String) = Scenario(
    name = scenarioName,
    detectionQuality = context.resources.getInteger(R.integer.default_detection_quality),
    endConditionOperator = OR,
)

/**
 * Creates a new event.
 *
 * @param context the Android context.
 * @return the new event.
 */
fun newDefaultEvent(context: Context, scenarioId: Long, scenarioEventsSize: Int) =
    Event(
        scenarioId = scenarioId,
        name = context.getString(R.string.default_event_name),
        conditionOperator = AND,
        priority = scenarioEventsSize,
        conditions = mutableListOf(),
        actions = mutableListOf(),
    )

/**
 * Creates a new default capture condition.
 * @param context the Android context.
 * @param eventId the event for this new action.
 * @param area the area of the condition to create.
 * @param bitmap the image for the condition to create.
 * @return the new condition.
 */
fun newDefaultConditionCapture(context: Context, eventId: Long, area: Rect, bitmap: Bitmap) =
    Condition.Capture(
        id = 0,
        eventId = eventId,
        priority = 0,
        name = context.resources.getString(R.string.default_condition_name),
        shouldBeDetected = true,
        bitmap = bitmap,
        area = area,
        detectArea = Rect(area),
        threshold = context.resources.getInteger(R.integer.default_condition_threshold),
        detectionType = EXACT,
    )

/**
 * Creates a new default process condition.
 * @param context the Android context.
 * @param eventId the event for this new action.
 * @return the new condition.
 */
fun newDefaultConditionProcess(context: Context, eventId: Long, processName: String = "") =
    Condition.Process(
        id = 0,
        eventId = eventId,
        priority = 0,
        name = context.resources.getString(R.string.default_condition_name),
        shouldBeDetected = true,
        processName = processName,
    )

/**
 * Creates a new default timer condition.
 * @param context the Android context.
 * @param eventId the event for this new action.
 * @return the new condition.
 */
fun newDefaultConditionTimer(context: Context, eventId: Long, defaultPeriod: Long = 0) =
    Condition.Timer(
        id = 0,
        eventId = eventId,
        priority = 0,
        name = context.resources.getString(R.string.default_condition_name),
        shouldBeDetected = true,
        period = defaultPeriod,
    )

/**
 * Creates a new default click action.
 * @param context the Android context.
 * @param eventId the event for this new action.
 * @return the new click.
 */
fun newDefaultClick(context: Context, eventId: Long) = Action.Click(
    eventId = eventId,
    priority = 0,
    name = context.getString(R.string.default_click_name),
    pressDuration = context.getEventConfigPreferences().getClickPressDurationConfig(context),
    clickOnCondition = false,
)

/**
 * Creates a new default swipe action.
 * @param context the Android context.
 * @param eventId the event for this new action.
 * @return the new swipe.
 */
fun newDefaultSwipe(context: Context, eventId: Long) = Action.Swipe(
    eventId = eventId,
    priority = 0,
    name = context.getString(R.string.default_swipe_name),
    swipeDuration = context.getEventConfigPreferences().getSwipeDurationConfig(context),
)

/**
 * Creates a new default pause action.
 * @param context the Android context.
 * @param eventId the event for this new action.
 * @return the new pause.
 */
fun newDefaultPause(context: Context, eventId: Long) = Action.Pause(
    eventId = eventId,
    priority = 0,
    name = context.getString(R.string.default_pause_name),
    pauseDuration = context.getEventConfigPreferences().getPauseDurationConfig(context)
)

/**
 * Creates a new default intent action.
 * @param context the Android context.
 * @param eventId the event for this new action.
 * @return the new intent.
 */
fun newDefaultIntent(context: Context, eventId: Long) = Action.Intent(
    eventId = eventId,
    priority = 0,
    name = context.getString(R.string.default_intent_name),
    isAdvanced = context.getEventConfigPreferences().getIntentIsAdvancedConfig(context),
)


const val TAG = "xihh"