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
import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.Repository
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
    val conditionList: Flow<List<Condition>?> = repository.getAllConditions()
        .combine(eventConditions) { dbConditions, eventConditions ->
            if (eventConditions == null) return@combine null

            val allConditions = mutableListOf<Condition>()
            allConditions.addAll(eventConditions)
            val otherEventConditions = dbConditions.toMutableList().apply {
                removeIf { allAction ->
                    eventConditions.find { allAction.id == it.id } != null
                }
            }
            allConditions.addAll(otherEventConditions)

            allConditions
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
}