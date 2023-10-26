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
package com.buzbuz.smartautoclicker.overlays.debugging

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.detection.DetectionResult
import com.buzbuz.smartautoclicker.engine.DetectorEngine
import com.buzbuz.smartautoclicker.overlays.utils.getDebugConfigPreferences
import com.buzbuz.smartautoclicker.overlays.utils.getIsDebugViewEnabled
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample

/** */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DebugModel(context: Context) : OverlayViewModel(context) {

    /** Debug configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = context.getDebugConfigPreferences()

    /** The detector engine. */
    private var detectorEngine: DetectorEngine = DetectorEngine.getDetectorEngine(context)

    /** The last result of detection. Only available if in debug detection. */
    private val debugLastResult = detectorEngine.debugEngine
        .flatMapLatest { it.lastResult }.onEach {
            println("DebugEngine:debugLastResult info(${it.detectionResult.isDetected})=condition(${it.event.name})=${it.condition} detectionResult=${it.detectionResult}")
        }

    /** Tells if the current detection is running in debug mode. */
    val isDebugging = detectorEngine.isDebugging.map { debugging ->
        debugging && sharedPreferences.getIsDebugViewEnabled(context)
    }

    /** The confidence rate on the last detection, positive or negative. */
    val debugLastConfidenceRate: Flow<String?> = debugLastResult
        .sample(CONFIDENCE_RATE_SAMPLING_TIME_MS)
        .map { lastDebugInfo ->
            (lastDebugInfo.detectionResult as? DetectionResult.Image)?.confidenceRate?.formatConfidenceRate()
        }

    /** The coordinates of the last positive detection. */
    val debugLastPositiveCoordinates: Flow<Rect> = debugLastResult
        .map { debugInfo ->
            if (debugInfo.detectionResult.isDetected) debugInfo.conditionArea
            else Rect()
        }

    /** The info on the last positive detection. */
    val debugLastPositive: Flow<LastPositiveDebugInfo> = detectorEngine.debugEngine
        .flatMapLatest { it.lastPositiveInfo }
        .flatMapLatest { debugInfo ->
            flow {
                emit(
                    LastPositiveDebugInfo(
                        debugInfo.event.name,
                        debugInfo.condition.name,
                        ((debugInfo.detectionResult as? DetectionResult.Image)?.confidenceRate
                            ?: 1.0).formatConfidenceRate(),
                    )
                )

                delay(POSITIVE_VALUE_DISPLAY_TIMEOUT_MS)
                emit(LastPositiveDebugInfo())
            }
        }

    /** True when a debug report is available. */
    val isDebugReportReady: Flow<Boolean> = detectorEngine.debugEngine
        .flatMapLatest { it.debugReport }
        .map { it != null }
}

/**
 * Info on the last positive detection.
 * @param eventName name of the event
 * @param conditionName the name of the condition detected.
 * @param confidenceRateText the text to display for the confidence rate
 */
data class LastPositiveDebugInfo(
    val eventName: String = "",
    val conditionName: String = "",
    val confidenceRateText: String = "",
)

/** Delay before removing the last positive result display in debug. */
private const val POSITIVE_VALUE_DISPLAY_TIMEOUT_MS = 1500L

/** Sampling on the current confidence rate for the display. */
private const val CONFIDENCE_RATE_SAMPLING_TIME_MS = 450L