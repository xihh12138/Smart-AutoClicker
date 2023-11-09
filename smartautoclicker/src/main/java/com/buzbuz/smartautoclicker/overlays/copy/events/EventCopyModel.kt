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
package com.buzbuz.smartautoclicker.overlays.copy.events

import android.content.Context

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.Scenario
import com.buzbuz.smartautoclicker.domain.CompleteScenario
import com.buzbuz.smartautoclicker.overlays.utils.getIconRes
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * View model for the [EventCopyDialog].
 *
 * @param context the Android context.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventCopyModel(context: Context) : OverlayViewModel(context) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(context)


    /** The currently searched event name. Null if no is. */
    private val scenarioId = MutableStateFlow<Long?>(null)

    /** The list of events for the configured scenario. They might be not all available yet in the database. */
    private val scenarioEvents = combine(repository.completeScenarios, scenarioId) { dbScenario, id ->
        dbScenario.find { it.scenario.id == id }?.events ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    /** The currently searched event name. Null if no is. */
    private val searchQuery = MutableStateFlow<String?>(null)

    /**
     * List of displayed event items.
     * This list can contains all events with headers, or the search result depending on the current search query.
     */
    val eventList: Flow<List<EventCopyItem>?> =
        combine(repository.completeScenarios, scenarioId, searchQuery) { dbScenarioEvents, scenarioId, query ->
            if (query.isNullOrEmpty()) {
                getAllItems(dbScenarioEvents, scenarioId)
            } else {
                getSearchedItems(dbScenarioEvents, query)
            }
        }

    /**
     * Get all items with the headers.
     * @param dbCompleteScenarios all complete scenarios in the database.
     * @param scenarioId all actions in the current event.
     * @return the complete list of action items.
     */
    private suspend fun getAllItems(
        dbCompleteScenarios: List<CompleteScenario>, scenarioId: Long?
    ): List<EventCopyItem> = withContext(Dispatchers.IO) {
        val allItems = mutableListOf<EventCopyItem>()

        // First, add the events from the current scenario
        val curScenarioWithEvents = if (scenarioId == null) {
            null
        } else {
            dbCompleteScenarios.find { it.scenario.id == scenarioId }
        }

        if (curScenarioWithEvents != null) {
            allItems.add(EventCopyItem.HeaderItem(context.getString(R.string.dialog_event_copy_header_event)))
            allItems.addAll(curScenarioWithEvents.events.sortedBy { it.priority }
                .map { it.toEventItem(curScenarioWithEvents.scenario) }.distinct())
        }

        // Then, add all other events. Remove the one already in this scenario.
        dbCompleteScenarios.forEach { scenarioWithEvents ->
            if (scenarioWithEvents.scenario.id != scenarioId) {
                allItems.add(EventCopyItem.HeaderItem(scenarioWithEvents.scenario.name))
                allItems.addAll(scenarioWithEvents.events.sortedBy { it.priority }
                    .map { it.toEventItem(scenarioWithEvents.scenario) }.distinct())
            }
        }

        allItems
    }

    /**
     * Get the result of the search query.
     * @param completeScenarios all complete scenarios in the database.
     * @param query the current search query.
     */
    private fun getSearchedItems(completeScenarios: List<CompleteScenario>, query: String): List<EventCopyItem> {
        val allItems = mutableListOf<EventCopyItem>()

        completeScenarios.forEach { completeScenario ->
            allItems.add(EventCopyItem.HeaderItem(completeScenario.scenario.name))
            allItems.addAll(completeScenario.events.filter { it.name.contains(query, true) }.sortedBy { it.priority }
                .map { it.toEventItem(completeScenario.scenario) }.distinct())
        }

        return allItems
    }

    /**
     * Set the current scenario events.
     * @param id the scenario identifier.
     */
    fun setCurrentScenario(id: Long) {
        scenarioId.value = id
    }

    /**
     * Update the events search query.
     * @param query the new query.
     */
    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
    }

    /**
     * Get a copy of the provided event.
     *
     * @param event the event to get the copy of.
     */
    fun getCopyEvent(event: Event): Event {
        return event.deepCopy().apply {
            priority = scenarioEvents.value.size
            scenarioId = this@EventCopyModel.scenarioId.value!!
            cleanUpIds()
        }
    }

    /** @return the [EventCopyItem.EventItem] corresponding to this event. */
    private fun Event.toEventItem(scenario: Scenario): EventCopyItem.EventItem {
        val item = EventCopyItem.EventItem(name, actions!!.map { it.getIconRes() }, scenario.name)
        item.event = this
        item.belongScenario = scenario
        return item
    }

    /** Types of items in the event copy list. */
    sealed class EventCopyItem {

        /**
         * Header item, delimiting sections.
         * @param title the title for the header.
         */
        data class HeaderItem(
            val title: String,
        ) : EventCopyItem()

        /**
         * Event item.
         * @param name the name of the event.
         * @param actions the icon resources for the actions of the event.
         * @param belongScenarioName the owner scenario's name of the event
         */
        data class EventItem(
            val name: String, val actions: List<Int>, val belongScenarioName: String
        ) : EventCopyItem() {

            /** Event represented by this item. */
            var event: Event? = null

            /** the owner scenario of the event */
            var belongScenario: Scenario? = null
        }
    }
}
