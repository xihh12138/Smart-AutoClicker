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
package com.buzbuz.smartautoclicker.engine

import com.buzbuz.smartautoclicker.domain.AND
import com.buzbuz.smartautoclicker.domain.ConditionOperator
import com.buzbuz.smartautoclicker.domain.EndCondition
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.OR

/**
 * Verifies if the scenario has reached its end conditions.
 *
 * @param conditions the list of the scenario end conditions.
 * @param conditionOperator the operator to apply to the conditions.
 * @param onEndConditionReached called when the scenario end conditions are fulfilled.
 */
internal class EndConditionVerifier(
    conditions: List<EndCondition>,
    @ConditionOperator private val conditionOperator: Int,
    private val onEndConditionReached: (EndCondition) -> Unit,
) {

    /** State of the executed events. EventId to Execution info.*/
    private val executedEvents = HashMap<Long, ExecutionInfo>().apply {
        conditions.forEach { condition ->
            put(condition.eventId, ExecutionInfo(endCondition = condition, maxExecutionCount = condition.executions))
        }
    }

    /** The list of already completed conditions. Used only for [AND]. */
    private val completedConditions: MutableList<Long> = mutableListOf()

    /**
     * Called when a event is triggered.
     * Increment the execution count for the event and verify if the end conditions are fulfilled.
     *
     * @param event the event triggered.
     * @return true if the end conditions are fulfilled, false if not.
     */
    suspend fun onEventTriggered(event: Event, onFinishEventTrigger: suspend (EndCondition) -> Unit): Boolean {
        // Is the event has a end condition ? If not, return false.
        val triggeredEventInfo = executedEvents[event.id] ?: return false
        val endCondition = triggeredEventInfo.endCondition

        // Increment the execution count and verify if this event end condition is fulfilled. If not, return false.
        triggeredEventInfo.executionCount++

        println("EndConditionVerifier: executionCount(${triggeredEventInfo.executionCount}/${triggeredEventInfo.maxExecutionCount}) event=$event")
        if (!triggeredEventInfo.conditionIsFulfilled()) return false
        println("EndConditionVerifier: conditionIsFulfilled endCondition=$endCondition")

        // If the operator is OR, we only need one condition fulfilled, notify end and return true.
        if (conditionOperator == OR) {
            onFinishEventTrigger(endCondition)
            onEndConditionReached(endCondition)
            return true
        }

        // If the operator is AND, add this end condition as reached, and verify if all conditions are now reached.
        completedConditions.add(event.id)
        if (completedConditions.size == executedEvents.size) {
            onFinishEventTrigger(endCondition)
            onEndConditionReached(endCondition)
            return true
        }
        return false
    }
}

/**
 * Execution information for an event.
 *
 * @param executionCount the number of execution for this event.
 * @param maxExecutionCount the number of execution to reach to fulfill the end condition.
 */
private data class ExecutionInfo(
    val endCondition: EndCondition,
    var executionCount: Int = 0,
    val maxExecutionCount: Int = 0,
) {

    /** @return true if the executionCount has reached the maxExecutionCount, false if not. */
    fun conditionIsFulfilled(): Boolean = executionCount >= maxExecutionCount
}