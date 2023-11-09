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
package com.buzbuz.smartautoclicker.overlays.copy.actions

import android.content.Context
import android.util.Log
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.CompleteScenario
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.Scenario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

/**
 * View model for the [ActionCopyDialog].
 *
 * @param context the Android context.
 */
class ActionCopyModel(context: Context) : OverlayViewModel(context) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(context)

    /** The list of actions for the configured event. They are not all available yet in the database. */
    private val eventActions = MutableStateFlow<List<Action>?>(null)

    /** The currently searched action name. Null if no is. */
    private val searchQuery = MutableStateFlow<String?>(null)

    /**
     * List of displayed action items.
     * This list can contains all events with headers, or the search result depending on the current search query.
     */
    val actionList: Flow<List<ActionCopyItem>?> =
        combine(repository.completeScenarios, eventActions, searchQuery) { completeScenarios, eventActions, query ->
            eventActions ?: return@combine null
            if (query.isNullOrEmpty()) {
                getAllItems(completeScenarios, eventActions)
            } else {
                getSearchedItems(completeScenarios, query)
            }
        }

    /**
     * Get all items with the headers.
     * @param completeScenarios all complete scenarios in the database.
     * @param eventActions all actions in the current event.
     * @return the complete list of action items.
     */
    private suspend fun getAllItems(
        completeScenarios: List<CompleteScenario>, eventActions: List<Action>
    ): List<ActionCopyItem> = withContext(Dispatchers.IO) {
        val allItems = mutableListOf<ActionCopyItem>()

        // First, add the actions from the current event
        var currentCompleteScenario: CompleteScenario? = null
        if (eventActions.isNotEmpty()) {
            val firstEventAction = eventActions.first()
            val currentEvent = completeScenarios.firstNotNullOfOrNull { completeScenario ->
                completeScenario.events.firstOrNull { event ->
                    event.actions?.contains(firstEventAction) == true
                }
            }
            currentCompleteScenario = completeScenarios.firstOrNull { completeScenario ->
                completeScenario.events.any { it == currentEvent }
            }

            // --- currentEvent could be null when creating a new event ---
            if (currentEvent != null && currentCompleteScenario != null) {
                allItems.add(ActionCopyItem.HeaderItem(context.getString(R.string.dialog_action_copy_header_scenario)))

                val currentScenario = currentCompleteScenario.scenario

                allItems.add(ActionCopyItem.SubHeaderItem(context.getString(R.string.dialog_action_copy_sub_header_event)))

                allItems.addAll(currentEvent.actions!!.sortedBy { it.priority }
                    .map { it.toActionItem(currentEvent, currentScenario) }.distinct())

                currentCompleteScenario.events.forEach { otherEvent ->
                    if (otherEvent != currentEvent) {
                        allItems.add(ActionCopyItem.SubHeaderItem(otherEvent.name))
                        allItems.addAll(
                            otherEvent.actions?.sortedBy { it.priority }
                                ?.map { it.toActionItem(otherEvent, currentScenario) }?.distinct()
                                ?: emptyList()
                        )
                    }
                }
            }

        }

        // --- Then,add other scenario's add ---
        completeScenarios.forEach { completeScenario ->
            if (completeScenario != currentCompleteScenario) {
                val currentScenario = completeScenario.scenario

                allItems.add(ActionCopyItem.HeaderItem(completeScenario.scenario.name))
                completeScenario.events.forEach { event ->
                    allItems.add(ActionCopyItem.SubHeaderItem(event.name))
                    allItems.addAll(
                        event.actions?.sortedBy { it.priority }?.map { it.toActionItem(event, currentScenario) }
                            ?.distinct() ?: emptyList()
                    )
                }
            }
        }

        allItems
    }

    /**
     * Get the result of the search query.
     * @param dbActions all actions in the database.
     * @param query the current search query.
     */
    private fun getSearchedItems(completeScenarios: List<CompleteScenario>, query: String): List<ActionCopyItem> {
        val allItems = mutableListOf<ActionCopyItem>()

        completeScenarios.forEach { completeScenario ->
            val currentScenario = completeScenario.scenario

            allItems.add(ActionCopyItem.HeaderItem(completeScenario.scenario.name))
            completeScenario.events.forEach { event ->
                allItems.add(ActionCopyItem.SubHeaderItem(event.name))
                allItems.addAll(event.actions?.filter { it.name?.contains(query, true) == true }
                    ?.sortedBy { it.priority }
                    ?.map { it.toActionItem(event, currentScenario) }?.distinct() ?: emptyList())
            }
        }

        return allItems
    }

    /**
     * Set the current event actions.
     * @param actions the actions.
     */
    fun setCurrentEventActions(actions: List<Action>) {
        eventActions.value = actions
    }

    /**
     * Update the action search query.
     * @param query the new query.
     */
    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
    }

    /**
     * Get a new action based on the provided one.
     * @param action the acton to copy.
     */
    fun getNewActionForCopy(action: Action): Action = when (action) {
        is Action.Click -> action.copy(id = 0, name = "" + action.name)
        is Action.Swipe -> action.copy(id = 0, name = "" + action.name)
        is Action.Pause -> action.copy(id = 0, name = "" + action.name)
        is Action.Intent -> action.copy(
            id = 0,
            name = "" + action.name,
            extras = action.extras?.map { it.copy(id = 0) }?.toMutableList()
        )
    }

    /** @return the [ActionCopyItem.ActionItem] corresponding to this action. */
    private fun Action.toActionItem(event: Event, scenario: Scenario): ActionCopyItem.ActionItem {
        val item = when (this) {
            is Action.Click -> ActionCopyItem.ActionItem(
                icon = R.drawable.ic_click,
                name = name!!,
                details = when (clickType) {
                    Action.Click.CLICK_TYPE_EXACT -> {
                        context.getString(
                            R.string.dialog_action_copy_click_details, formatDuration(pressDuration!!), x, y
                        )
                    }

                    Action.Click.CLICK_TYPE_CONDITION -> {
                        context.getString(R.string.dialog_action_config_click_position_on_condition)
                    }

                    Action.Click.CLICK_TYPE_RANDOM -> {
                        context.getString(
                            R.string.dialog_action_copy_click_random,
                            formatDuration(pressDuration!!),
                            randomArea?.left,
                            randomArea?.top,
                            randomArea?.right,
                            randomArea?.bottom
                        )
                    }

                    else -> {
                        if (clickOnCondition) context.getString(R.string.dialog_action_config_click_position_on_condition)
                        else context.getString(
                            R.string.dialog_action_copy_click_details, formatDuration(pressDuration!!), x, y
                        )
                    }
                },
            )

            is Action.Swipe -> ActionCopyItem.ActionItem(
                icon = R.drawable.ic_swipe,
                name = name!!,
                details = context.getString(
                    R.string.dialog_action_copy_swipe_details, formatDuration(swipeDuration!!), fromX, fromY, toX, toY
                ),
            )

            is Action.Pause -> ActionCopyItem.ActionItem(
                icon = R.drawable.ic_wait,
                name = name!!,
                details = context.getString(
                    R.string.dialog_action_copy_pause_details, formatDuration(pauseDuration!!)
                ),
            )

            is Action.Intent -> ActionCopyItem.ActionItem(
                icon = R.drawable.ic_intent,
                name = name!!,
                details = formatIntentDetails(this),
            )
        }

        item.action = this
        item.belongEvent = event
        item.belongScenario = scenario
        return item
    }

    /**
     * Format a duration into a human readable string.
     * @param msDuration the duration to be formatted in milliseconds.
     * @return the formatted duration.
     */
    @OptIn(ExperimentalTime::class)
    private fun formatDuration(msDuration: Long): String {
        val duration = msDuration.milliseconds
        var value = ""
        if (duration.inWholeHours > 0) {
            value += "${duration.inWholeHours}h "
        }
        if (duration.inWholeMinutes % 60 > 0) {
            value += "${duration.inWholeMinutes % 60}m"
        }
        if (duration.inWholeSeconds % 60 > 0) {
            value += "${duration.inWholeSeconds % 60}s"
        }
        if (duration.inWholeMilliseconds % 1000 > 0) {
            value += "${duration.inWholeMilliseconds % 1000}ms"
        }

        return value.trim()
    }

    /**
     * Format a action intent into a human readable string.
     * @param intent the action intent to be formatted.
     * @return the formatted intent.
     */
    private fun formatIntentDetails(intent: Action.Intent): String {
        var action = intent.intentAction ?: let {
            Log.w(TAG, "formatIntentDetails: null intent action !")
            return ""
        }

        val dotIndex = action.lastIndexOf('.')
        if (dotIndex != -1 && dotIndex < action.lastIndex) {
            action = action.substring(dotIndex + 1)

            if (intent.isBroadcast == false && intent.componentName != null && action.length < INTENT_COMPONENT_DISPLAYED_ACTION_LENGTH_LIMIT) {

                var componentName = intent.componentName!!.flattenToString()
                val dotIndex2 = componentName.lastIndexOf('.')
                if (dotIndex2 != -1 && dotIndex2 < componentName.lastIndex) {

                    componentName = componentName.substring(dotIndex2 + 1)
                    if (componentName.length < INTENT_COMPONENT_DISPLAYED_COMPONENT_LENGTH_LIMIT) {
                        return context.getString(
                            R.string.dialog_action_copy_intent_details_action_component,
                            action,
                            componentName,
                        )
                    }
                }
            }
        }

        return context.getString(R.string.dialog_action_copy_intent_details_action, action)
    }

    /** Types of items in the action copy list. */
    sealed class ActionCopyItem {

        /**
         * Header item, delimiting sections.
         * @param title the title for the header.
         */
        data class HeaderItem(
            val title: String,
        ) : ActionCopyItem()

        /**
         * Sub header item, delimiting sections.
         * @param title the title for the header.
         */
        data class SubHeaderItem(
            val title: String,
        ) : ActionCopyItem()

        /**
         * Action item.
         * @param icon the icon for the action.
         * @param name the name of the action.
         * @param details the details for the action.
         */
        data class ActionItem(
            @DrawableRes val icon: Int,
            val name: String,
            val details: String,
        ) : ActionCopyItem() {

            /** Action represented by this item. */
            var action: Action? = null

            /** The event to which the [action] belongs  */
            var belongEvent: Event? = null

            /** The scenario to which the [belongEvent] belongs  */
            var belongScenario: Scenario? = null
        }
    }
}

/** */
private const val INTENT_COMPONENT_DISPLAYED_ACTION_LENGTH_LIMIT = 15

/** */
private const val INTENT_COMPONENT_DISPLAYED_COMPONENT_LENGTH_LIMIT = 20

/** Tag for logs. */
private const val TAG = "ActionCopyModel"