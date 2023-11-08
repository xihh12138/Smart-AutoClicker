/*
 * Copyright (C) 2021 Nain57
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
package com.buzbuz.smartautoclicker.overlays.copy.conditions

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.domain.CompleteScenario
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.Scenario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View model for the [ConditionCopyDialog].
 * @param context the Android context.
 */
class ConditionCopyModel(context: Context) : OverlayViewModel(context) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(context)

    /** The list of condition for the configured event. They are not all available yet in the database. */
    private val eventConditions = MutableStateFlow<List<Condition>?>(null)

    /** List of all conditions. */
    val conditionList: Flow<List<ConditionCopyItem>?> = repository.completeScenarios
        .combine(eventConditions) { completeScenarios, eventConditions ->
            if (eventConditions == null) return@combine null

            val allItems = mutableListOf<ConditionCopyItem>()

            // First, add the actions from the current event
            var currentCompleteScenario: CompleteScenario? = null
            if (eventConditions.isNotEmpty()) {
                val firstEventAction = eventConditions.first()
                val currentEvent = completeScenarios.firstNotNullOf { completeScenario ->
                    completeScenario.events.firstOrNull { event ->
                        event.conditions?.contains(firstEventAction) == true
                    }
                }
                currentCompleteScenario = completeScenarios.first { completeScenario ->
                    completeScenario.events.any { it == currentEvent }
                }

                val currentScenario = currentCompleteScenario.scenario

                allItems.add(ConditionCopyItem.HeaderItem(context.getString(R.string.dialog_action_copy_header_scenario)))
                allItems.add(ConditionCopyItem.SubHeaderItem(context.getString(R.string.dialog_action_copy_sub_header_event)))
                allItems.addAll(currentEvent.conditions!!.sortedBy { it.priority }
                    .map { it.toConditionItem(currentEvent, currentScenario) }
                    .distinct())

                currentCompleteScenario.events.forEach { otherEvent ->
                    if (otherEvent != currentEvent) {
                        allItems.add(ConditionCopyItem.SubHeaderItem(otherEvent.name))
                        allItems.addAll(otherEvent.conditions?.sortedBy { it.priority }
                            ?.map { it.toConditionItem(otherEvent, currentScenario) }
                            ?.distinct() ?: emptyList())
                    }
                }
            }

            completeScenarios.forEach { completeScenario ->
                if (completeScenario != currentCompleteScenario) {
                    val currentScenario = completeScenario.scenario

                    allItems.add(ConditionCopyItem.HeaderItem(completeScenario.scenario.name))
                    completeScenario.events.forEach { event ->
                        allItems.add(ConditionCopyItem.SubHeaderItem(event.name))
                        allItems.addAll(event.conditions?.sortedBy { it.priority }
                            ?.map { it.toConditionItem(event, currentScenario) }
                            ?.distinct() ?: emptyList())
                    }
                }
            }

            allItems
        }

    /**
     * Set the current event conditions.
     * @param conditions the conditions.
     */
    fun setCurrentEventConditions(conditions: List<Condition>) {
        eventConditions.value = conditions
    }

    /**
     * Get a new condition based on the provided one.
     * @param condition the condition to copy.
     */
    fun getNewConditionForCopy(condition: Condition): Condition = when (condition) {
        is Condition.Capture -> condition.copy(
            id = 0,
            name = "" + condition.name,
            path = if (condition.path != null) "" + condition.path else null
        )

        is Condition.Process -> condition.copy(id = 0, name = "" + condition.name)
    }

    /**
     * Get the bitmap corresponding to a condition.
     * Loading is async and the result notified via the onBitmapLoaded argument.
     *
     * @param condition the condition to load the bitmap of.
     * @param onBitmapLoaded the callback notified upon completion.
     */
    fun getConditionBitmap(condition: Condition, onBitmapLoaded: (Bitmap?) -> Unit): Job? {
        when (condition) {
            is Condition.Capture -> {
                if (condition.bitmap != null) {
                    onBitmapLoaded(condition.bitmap)
                    return null
                }

                if (condition.path != null) {
                    return viewModelScope.launch(Dispatchers.IO) {
                        val bitmap =
                            repository.getBitmap(condition.path!!, condition.area.width(), condition.area.height())

                        if (isActive) {
                            withContext(Dispatchers.Main) {
                                onBitmapLoaded(bitmap)
                            }
                        }
                    }
                }
            }

            is Condition.Process -> return viewModelScope.launch {
                try {
                    onBitmapLoaded(context.packageManager.getApplicationIcon(condition.processName).toBitmap())
                } catch (e: Exception) {
                    onBitmapLoaded(null)
                    e.printStackTrace()
                }
            }
        }

        onBitmapLoaded(null)

        return null
    }

    private fun Condition.toConditionItem(event: Event, scenario: Scenario): ConditionCopyItem.ConditionItem {
        val item = ConditionCopyItem.ConditionItem(this).apply {
            belongEvent = event
            belongScenario = scenario
        }

        return item
    }

    /** Types of items in the action copy list. */
    sealed class ConditionCopyItem {

        /**
         * Header item, delimiting sections.
         * @param title the title for the header.
         */
        data class HeaderItem(
            val title: String,
        ) : ConditionCopyItem()

        /**
         * Sub header item, delimiting sections.
         * @param title the title for the header.
         */
        data class SubHeaderItem(
            val title: String,
        ) : ConditionCopyItem()

        /**
         * Condition item.
         * @param condition  Condition represented by this item.
         */
        data class ConditionItem(
            val condition: Condition
        ) : ConditionCopyItem() {

            /** The event to which the [condition] belongs  */
            var belongEvent: Event? = null

            /** The scenario to which the [belongEvent] belongs  */
            var belongScenario: Scenario? = null
        }
    }
}