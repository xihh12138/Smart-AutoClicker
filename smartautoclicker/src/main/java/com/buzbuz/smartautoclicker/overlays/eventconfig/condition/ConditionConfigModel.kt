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
package com.buzbuz.smartautoclicker.overlays.eventconfig.condition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.core.graphics.drawable.toBitmap
import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.overlays.eventconfig.condition.capture.CaptureConfigModel
import com.buzbuz.smartautoclicker.overlays.eventconfig.condition.process.ProcessConfigModel
import com.buzbuz.smartautoclicker.overlays.eventconfig.condition.timer.TimerConfigModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

/**
 * View model for the [ConditionConfigDialog].
 * @param context the Android context.
 */
class ConditionConfigModel(context: Context) : OverlayViewModel(context) {

    /** Repository providing access to the database. */
    private val repository = Repository.getRepository(context)

    /** The condition being configured by the user. Defined using [setConfigCondition]. */
    private val configuredCondition = MutableStateFlow<Condition?>(null)

    /** The type of detection currently selected by the user. */
    val name: Flow<String?> = configuredCondition.map { it?.name }.take(1)

    /** Tells if the condition should be present or not on the screen. */
    val shouldBeDetected: Flow<Boolean> = configuredCondition.mapNotNull { it?.shouldBeDetected }

    /** The model for the [configuredCondition]. Type will change according to the action type. */
    val conditionModel: StateFlow<ConditionModel?> = configuredCondition
        .map { condition ->
            @Suppress("UNCHECKED_CAST") // Nullity is handled first
            when (condition) {
                null -> null
                is Condition.Capture ->
                    CaptureConfigModel(viewModelScope, configuredCondition as MutableStateFlow<Condition.Capture?>)

                is Condition.Process ->
                    ProcessConfigModel(viewModelScope, configuredCondition as MutableStateFlow<Condition.Process?>)

                is Condition.Timer ->
                    TimerConfigModel(viewModelScope, configuredCondition as MutableStateFlow<Condition.Timer?>)
            }
        }
        .take(1)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            null
        )

    /** Tells if the configured condition is valid and can be saved. */
    val isValidCondition: Flow<Boolean> = conditionModel.flatMapConcat { it?.isValidCondition ?: flow { emit(false) } }

    /**
     * Set the configured condition.
     * This will update all values represented by this view model.
     *
     * @param condition the condition to configure.
     */
    fun setConfigCondition(condition: Condition) {
        viewModelScope.launch {
            configuredCondition.emit(condition.deepCopy())
        }
    }

    /** @return the condition containing all user changes. */
    fun getConfiguredCondition(): Condition =
        configuredCondition.value
            ?: throw IllegalStateException("Can't get the configured condition, none were defined.")

    /**
     * Set the configured condition name.
     * @param name the new condition name.
     */
    fun setName(name: String) {
        configuredCondition.value?.let { condition ->
            configuredCondition.value = when (condition) {
                is Condition.Capture -> condition.copy(name = name)
                is Condition.Process -> condition.copy(name = name)
                is Condition.Timer -> condition.copy(name = name)
            }
        } ?: throw IllegalStateException("Can't set condition name, condition is null!")
    }

    /** Toggle between true and false for the shouldBeDetected value of the condition. */
    fun toggleShouldBeDetected() {
        configuredCondition.value?.let { condition ->
            configuredCondition.value = when (condition) {
                is Condition.Capture -> condition.copy(shouldBeDetected = !condition.shouldBeDetected)
                is Condition.Process -> condition.copy(shouldBeDetected = !condition.shouldBeDetected)
                is Condition.Timer -> condition.copy(shouldBeDetected = !condition.shouldBeDetected)
            }
        } ?: throw IllegalStateException("Can't toggle condition should be detected, condition is null!")
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

            is Condition.Timer -> return null
        }

        onBitmapLoaded(null)

        return null
    }
}

/** Base class for observing/editing the values for an action. */
abstract class ConditionModel {

    /** True if the action values are correct, false if not. */
    abstract val isValidCondition: Flow<Boolean>

}

/** The maximum threshold value selectable by the user. */
const val MAX_THRESHOLD = 20

data class DetectionTypeAndValue(val type: Int, val area: Rect)