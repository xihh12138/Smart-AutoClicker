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
package com.buzbuz.smartautoclicker.backup

import android.graphics.Point
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.buzbuz.smartautoclicker.backup.ext.getBoolean
import com.buzbuz.smartautoclicker.backup.ext.getEnum
import com.buzbuz.smartautoclicker.backup.ext.getInt
import com.buzbuz.smartautoclicker.backup.ext.getJsonArray
import com.buzbuz.smartautoclicker.backup.ext.getJsonObject
import com.buzbuz.smartautoclicker.backup.ext.getLong
import com.buzbuz.smartautoclicker.backup.ext.getString
import com.buzbuz.smartautoclicker.database.room.CLICK_DATABASE_VERSION
import com.buzbuz.smartautoclicker.database.room.entity.ActionEntity
import com.buzbuz.smartautoclicker.database.room.entity.ActionType
import com.buzbuz.smartautoclicker.database.room.entity.ClickType
import com.buzbuz.smartautoclicker.database.room.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.database.room.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.database.room.entity.CompleteScenario
import com.buzbuz.smartautoclicker.database.room.entity.ConditionEntity
import com.buzbuz.smartautoclicker.database.room.entity.ConditionType
import com.buzbuz.smartautoclicker.database.room.entity.EndConditionEntity
import com.buzbuz.smartautoclicker.database.room.entity.EventEntity
import com.buzbuz.smartautoclicker.database.room.entity.IntentExtraEntity
import com.buzbuz.smartautoclicker.database.room.entity.IntentExtraType
import com.buzbuz.smartautoclicker.database.room.entity.ScenarioEntity
import com.buzbuz.smartautoclicker.database.room.entity.sortByPriority
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.jsonObject
import java.io.InputStream
import java.io.OutputStream

/**
 * Serializer/Deserializer for database scenarios (json).
 * It (tries to) handles the compatibility by deserializing manually if the version isn't the same.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class ScenarioSerializer {

    /**
     * Serialize a scenario.
     *
     * @param scenario the scenario to serialize.
     * @param screenSize this device screen size.
     * @param outputStream the stream to serialize into.
     */
    fun serialize(scenario: CompleteScenario, screenSize: Point, outputStream: OutputStream) =
        Json.encodeToStream(ScenarioBackup(CLICK_DATABASE_VERSION, screenSize.x, screenSize.y, scenario), outputStream)

    /**
     * Deserialize a scenario.
     * Depending of the detected version, either kotlin or compat serialization will be used.
     *
     * @param json the stream to deserialize from.
     *
     * @return the scenario backup deserialized from the json.
     */
    fun deserialize(json: InputStream): ScenarioBackup? {
        val jsonBackup = Json.parseToJsonElement(json.readBytes().toString(Charsets.UTF_8)).jsonObject
        val version = jsonBackup.getInt("version", true) ?: -1
        val scenario = when {
            version < 8 -> {
                Log.w(TAG, "Can't deserialize scenario, invalid version.")
                null
            }

            version == CLICK_DATABASE_VERSION -> {
                Log.d(TAG, "Current version, use standard serialization.")
                Json.decodeFromJsonElement<ScenarioBackup>(jsonBackup).scenario
            }

            else -> {
                Log.d(TAG, "Not the current version, use compat serialization.")
                jsonBackup.deserializeCompleteScenarioCompat()
            }
        }?.run {
            copy(
                events = events.map { completeEventEntity ->
                    completeEventEntity.copy(
                        actions = completeEventEntity.actions.sortByPriority(),
                        conditions = completeEventEntity.conditions.sortByPriority(),
                    )
                }
            )
        }

        if (scenario == null) {
            Log.w(TAG, "Can't deserialize scenario.")
            return null
        }

        return ScenarioBackup(
            version = version,
            screenWidth = jsonBackup.getInt("screenWidth") ?: 0,
            screenHeight = jsonBackup.getInt("screenHeight") ?: 0,
            scenario = scenario,
        )
    }

    /**
     * Manual deserialization called when the version differs.
     * Tries to do a "best effort" deserialization of the provided json in order to keep as much backward
     * compatibility as possible.
     *
     * @return the complete scenario, if the deserialization is a success, or null if not.
     */
    @VisibleForTesting
    internal fun JsonObject.deserializeCompleteScenarioCompat(): CompleteScenario? {
        val jsonCompleteScenario = getJsonObject("scenario") ?: return null

        val scenario: ScenarioEntity = jsonCompleteScenario.getJsonObject("scenario")?.deserializeScenarioCompat()
            ?: return null

        return CompleteScenario(
            scenario = scenario,
            events = jsonCompleteScenario.getJsonArray("events")?.deserializeCompleteEventCompat()
                ?: emptyList(),
            endConditions = jsonCompleteScenario.getJsonArray("endConditions")?.deserializeEndConditionsCompat()
                ?: emptyList()
        )
    }

    /** @return the deserialized scenario. */
    @VisibleForTesting
    internal fun JsonObject.deserializeScenarioCompat(): ScenarioEntity? {
        val id = getLong("id", true) ?: return null

        return ScenarioEntity(
            id = id,
            name = getString("name") ?: "",
            detectionQuality = getInt("detectionQuality")
                ?.coerceIn(DETECTION_QUALITY_LOWER_BOUND, DETECTION_QUALITY_UPPER_BOUND)
                ?: DETECTION_QUALITY_DEFAULT_VALUE,
            endConditionOperator = getInt("endConditionOperator")
                ?.coerceIn(OPERATOR_LOWER_BOUND, OPERATOR_UPPER_BOUND)
                ?: OPERATOR_DEFAULT_VALUE,
        )
    }

    /** @return the deserialized end condition list. */
    @VisibleForTesting
    internal fun JsonArray.deserializeEndConditionsCompat(): List<EndConditionEntity> = mapNotNull { endCondition ->
        with(endCondition.jsonObject) {
            val id = getLong("id", true) ?: return@mapNotNull null
            val scenarioId = getLong("scenarioId", true) ?: return@mapNotNull null
            val eventId = getLong("eventId", true) ?: return@mapNotNull null

            EndConditionEntity(
                id = id,
                scenarioId = scenarioId,
                eventId = eventId,
                executions = getInt("executions") ?: END_CONDITION_EXECUTION_DEFAULT_VALUE
            )
        }
    }

    /** @return the deserialized complete event list. */
    @VisibleForTesting
    internal fun JsonArray.deserializeCompleteEventCompat(): List<CompleteEventEntity> = mapNotNull { completeEvent ->
        with(completeEvent.jsonObject) {
            val event = getJsonObject("event", true)?.deserializeEventCompat()
                ?: return@mapNotNull null

            val conditions = getJsonArray("conditions")?.deserializeConditionsCompat()
            if (conditions.isNullOrEmpty()) {
                Log.i(TAG, "Can't deserialize this complete event, there is no conditions")
                return@mapNotNull null
            }

            val completeActions = getJsonArray("actions")?.deserializeCompleteActionsCompat()
            if (completeActions.isNullOrEmpty()) {
                Log.i(TAG, "Can't deserialize this complete event, there is no actions")
                return@mapNotNull null
            }

            CompleteEventEntity(event, completeActions, conditions)
        }
    }

    /** @return the deserialized event. */
    @VisibleForTesting
    internal fun JsonObject.deserializeEventCompat(): EventEntity? {
        val id = getLong("id", true) ?: return null
        val scenarioId = getLong("scenarioId", true) ?: return null

        return EventEntity(
            id = id,
            scenarioId = scenarioId,
            name = getString("name") ?: "",
            conditionOperator = getInt("conditionOperator")
                ?.coerceIn(OPERATOR_LOWER_BOUND, OPERATOR_UPPER_BOUND)
                ?: OPERATOR_DEFAULT_VALUE,
            priority = getInt("priority")?.coerceAtLeast(0) ?: 0,
            stopAfter = getInt("stopAfter")?.coerceAtLeast(0),
        )
    }

    /** @return the deserialized condition list. */
    @VisibleForTesting
    internal fun JsonArray.deserializeConditionsCompat(): List<ConditionEntity> = mapNotNull { condition ->
        with(condition.jsonObject) {
            when (getEnum<ConditionType>("type", true)) {
                ConditionType.CAPTURE -> deserializeCaptureConditionCompat()
                ConditionType.PROCESS -> deserializeProcessConditionCompat()
                null -> deserializeCaptureConditionCompat()
            }
        }
    }


    internal fun JsonObject.deserializeCaptureConditionCompat(): ConditionEntity? {
        val id = getLong("id", true) ?: return null
        val eventId = getLong("eventId", true) ?: return null
        val priority = getInt("priority")?.coerceAtLeast(0) ?: 0
        val name = getString("name") ?: ""
        val shouldBeDetected = getBoolean("shouldBeDetected") ?: true

        val path = getString("path", true) ?: return null
        val areaLeft = getInt("areaLeft", true) ?: return null
        val areaTop = getInt("areaTop", true) ?: return null
        val areaRight = getInt("areaRight", true) ?: return null
        val areaBottom = getInt("areaBottom", true) ?: return null
        val detectAreaLeft = getInt("detectAreaLeft", true) ?: areaLeft
        val detectAreaTop = getInt("detectAreaTop", true) ?: areaTop
        val detectAreaRight = getInt("detectAreaRight", true) ?: areaRight
        val detectAreaBottom = getInt("detectAreaBottom", true) ?: areaBottom

        return ConditionEntity(
            id = id,
            eventId = eventId,
            priority = priority,
            name = name,
            type = ConditionType.CAPTURE,
            shouldBeDetected = shouldBeDetected,
            path = path,
            areaLeft = areaLeft,
            areaTop = areaTop,
            areaRight = areaRight,
            areaBottom = areaBottom,
            detectAreaLeft = detectAreaLeft,
            detectAreaTop = detectAreaTop,
            detectAreaRight = detectAreaRight,
            detectAreaBottom = detectAreaBottom,
            threshold = getInt("threshold")
                ?.coerceIn(CONDITION_THRESHOLD_LOWER_BOUND, CONDITION_THRESHOLD_UPPER_BOUND)
                ?: CONDITION_THRESHOLD_DEFAULT_VALUE,
            detectionType = getInt("detectionType")
                ?.coerceIn(DETECTION_TYPE_LOWER_BOUND, DETECTION_TYPE_UPPER_BOUND)
                ?: DETECTION_TYPE_DEFAULT_VALUE,
        )
    }

    internal fun JsonObject.deserializeProcessConditionCompat(): ConditionEntity? {
        val id = getLong("id", true) ?: return null
        val eventId = getLong("eventId", true) ?: return null
        val priority = getInt("priority")?.coerceAtLeast(0) ?: 0
        val name = getString("name") ?: ""
        val shouldBeDetected = getBoolean("shouldBeDetected") ?: true

        val processName = getString("processName", true) ?: return null

        return ConditionEntity(
            id = id,
            eventId = eventId,
            priority = priority,
            name = name,
            type = ConditionType.PROCESS,
            shouldBeDetected = shouldBeDetected,
            processName = processName
        )
    }


    /** @return the deserialized complete action list. */
    @VisibleForTesting
    internal fun JsonArray.deserializeCompleteActionsCompat(): List<CompleteActionEntity> =
        mapNotNull { completeActions ->
            with(completeActions.jsonObject) {
                val action = getJsonObject("action")?.deserializeActionCompat() ?: return@mapNotNull null

                CompleteActionEntity(
                    action = action,
                    intentExtras = getJsonArray("intentExtras")?.deserializeIntentExtrasCompat() ?: emptyList()
                )
            }
        }

    /** @return the deserialized action. */
    @VisibleForTesting
    internal fun JsonObject.deserializeActionCompat(): ActionEntity? =
        when (getEnum<ActionType>("type", true)) {
            ActionType.CLICK -> deserializeClickActionCompat()
            ActionType.SWIPE -> deserializeSwipeActionCompat()
            ActionType.PAUSE -> deserializePauseActionCompat()
            ActionType.INTENT -> deserializeIntentActionCompat()
            null -> null
        }

    /** @return the deserialized click action. */
    @VisibleForTesting
    internal fun JsonObject.deserializeClickActionCompat(): ActionEntity? {
        val id = getLong("id", true) ?: return null
        val eventId = getLong("eventId", true) ?: return null

        val x: Int?
        val y: Int?
        val clickType = getEnum<ClickType>("clickType")
        val clickOnCondition = getBoolean("clickOnCondition") ?: false

        when (clickType) {
            ClickType.EXACT -> {
                x = getInt("x", true) ?: return null
                y = getInt("y", true) ?: return null
            }

            ClickType.CONDITION -> {
                x = null
                y = null
            }

            ClickType.RANDOM -> {
                x = null
                y = null
            }

            null -> {
                // ---------- old version depends on filed "clickOnCondition"----------
                if (clickOnCondition) {
                    x = null
                    y = null
                } else {
                    x = getInt("x", true) ?: return null
                    y = getInt("y", true) ?: return null
                }
            }
        }

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = getString("name") ?: "",
            priority = getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.CLICK,
            clickOnCondition = clickOnCondition,
            x = x,
            y = y,
            pressDuration = getLong("pressDuration")?.coerceAtLeast(0) ?: DEFAULT_CLICK_DURATION,
            clickType = clickType,
            clickRandomAreaLeft = getInt("clickRandomAreaLeft"),
            clickRandomAreaTop = getInt("clickRandomAreaTop"),
            clickRandomAreaRight = getInt("clickRandomAreaRight"),
            clickRandomAreaBottom = getInt("clickRandomAreaBottom"),
        )
    }

    /** @return the deserialized swipe action. */
    @VisibleForTesting
    internal fun JsonObject.deserializeSwipeActionCompat(): ActionEntity? {
        val id = getLong("id", true) ?: return null
        val eventId = getLong("eventId", true) ?: return null

        val fromX = getInt("fromX", true) ?: return null
        val fromY = getInt("fromY", true) ?: return null
        val toX = getInt("toX", true) ?: return null
        val toY = getInt("toY", true) ?: return null

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = getString("name") ?: "",
            priority = getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.SWIPE,
            fromX = fromX,
            fromY = fromY,
            toX = toX,
            toY = toY,
            swipeDuration = getLong("swipeDuration")?.coerceAtLeast(0) ?: DEFAULT_SWIPE_DURATION,
        )
    }

    /** @return the deserialized pause action. */
    @VisibleForTesting
    internal fun JsonObject.deserializePauseActionCompat(): ActionEntity? {
        val id = getLong("id", true) ?: return null
        val eventId = getLong("eventId", true) ?: return null

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = getString("name") ?: "",
            priority = getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.PAUSE,
            pauseDuration = getLong("pauseDuration")?.coerceAtLeast(0) ?: DEFAULT_PAUSE_DURATION,
        )
    }

    /** @return the deserialized intent action. */
    @VisibleForTesting
    internal fun JsonObject.deserializeIntentActionCompat(): ActionEntity? {
        val id = getLong("id", true) ?: return null
        val eventId = getLong("eventId", true) ?: return null
        val intentAction = getString("intentAction", true) ?: return null

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = getString("name") ?: "",
            priority = getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.INTENT,
            isAdvanced = getBoolean("isAdvanced") ?: false,
            isBroadcast = getBoolean("isBroadcast") ?: false,
            intentAction = intentAction,
            componentName = getString("componentName"),
            flags = getInt("flags")?.coerceAtLeast(0) ?: 0,
        )
    }

    /** @return the deserialized intent extra. */
    @VisibleForTesting
    internal fun JsonArray.deserializeIntentExtrasCompat(): List<IntentExtraEntity> = mapNotNull { extra ->
        with(extra.jsonObject) {
            val id = getLong("id", true) ?: return@mapNotNull null
            val actionId = getLong("actionId", true) ?: return@mapNotNull null
            val type = getEnum<IntentExtraType>("type", true) ?: return@mapNotNull null
            val key = getString("key", true) ?: return@mapNotNull null
            val value = getString("value", true) ?: return@mapNotNull null

            IntentExtraEntity(id, actionId, type, key, value)
        }
    }
}

/** Scenario detection quality lower bound on compat deserialization. */
const val DETECTION_QUALITY_LOWER_BOUND = 400

/** Scenario detection quality upper bound on compat deserialization. */
const val DETECTION_QUALITY_UPPER_BOUND = 1200

/** Scenario detection quality default value on compat deserialization. */
const val DETECTION_QUALITY_DEFAULT_VALUE = 600

/** Operators lower bound on compat deserialization. */
const val OPERATOR_LOWER_BOUND = 1

/** Operators upper bound on compat deserialization. */
const val OPERATOR_UPPER_BOUND = 2

/** Operators default value on compat deserialization. */
const val OPERATOR_DEFAULT_VALUE = OPERATOR_LOWER_BOUND

/** Detection type lower bound on compat deserialization. */
const val DETECTION_TYPE_LOWER_BOUND = 1

/** Detection type upper bound on compat deserialization. */
const val DETECTION_TYPE_UPPER_BOUND = 2

/** Detection type default value on compat deserialization. */
const val DETECTION_TYPE_DEFAULT_VALUE = DETECTION_TYPE_LOWER_BOUND

/** Condition threshold lower bound on compat deserialization. */
const val CONDITION_THRESHOLD_LOWER_BOUND = 0

/** Condition threshold upper bound on compat deserialization. */
const val CONDITION_THRESHOLD_UPPER_BOUND = 20

/** Condition threshold default value on compat deserialization. */
const val CONDITION_THRESHOLD_DEFAULT_VALUE = 4

/** End condition executions default value on compat deserialization. */
const val END_CONDITION_EXECUTION_DEFAULT_VALUE = 1

/** Default click duration in ms on compat deserialization. */
const val DEFAULT_CLICK_DURATION = 1L

/** Default swipe duration in ms on compat deserialization. */
const val DEFAULT_SWIPE_DURATION = 250L

/** Default pause duration in ms on compat deserialization. */
const val DEFAULT_PAUSE_DURATION = 50L

/** Tag for logs. */
private const val TAG = "ScenarioDeserializer"