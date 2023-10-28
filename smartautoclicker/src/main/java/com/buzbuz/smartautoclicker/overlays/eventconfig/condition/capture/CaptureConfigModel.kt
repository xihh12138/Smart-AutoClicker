package com.buzbuz.smartautoclicker.overlays.eventconfig.condition.capture

import android.graphics.Rect
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.DETECT_AREA
import com.buzbuz.smartautoclicker.domain.EXACT
import com.buzbuz.smartautoclicker.overlays.eventconfig.condition.ConditionModel
import com.buzbuz.smartautoclicker.overlays.eventconfig.condition.DetectionTypeAndValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class CaptureConfigModel(
    private val viewModelScope: CoroutineScope,
    private val configuredCondition: MutableStateFlow<Condition.Capture?>,
) : ConditionModel() {

    /** The type of detection currently selected by the user. */
    val detectionTypeAndValue: Flow<DetectionTypeAndValue> =
        configuredCondition.filterNotNull().map { DetectionTypeAndValue(it.detectionType, it.detectArea) }

    /** The condition threshold value currently edited by the user. */
    val threshold: Flow<Int> = configuredCondition.mapNotNull { it?.threshold }

    override val isValidCondition: Flow<Boolean> = configuredCondition.map { condition ->
        condition != null && condition.name.isNotEmpty() && condition.detectArea.width() >= condition.area.width() && condition.detectArea.height() >= condition.area.height()
    }

    /** Toggle between exact and whole screen for the detection type. */
    fun toggleDetectionType() {
        configuredCondition.value?.let { condition ->
            configuredCondition.value = condition.copy(
                detectionType = if (condition.detectionType == DETECT_AREA) EXACT else condition.detectionType + 1,
            )
        } ?: throw IllegalStateException("Can't toggle condition should be detected, condition is null!")
    }

    /** Toggle between exact and whole screen for the detection type. */
    fun setDetectionArea(detectionArea: Rect) {
        configuredCondition.value?.let { condition ->
            configuredCondition.value = condition.copy(detectArea = detectionArea)
        } ?: throw IllegalStateException("Can't toggle condition should be detected, condition is null!")
    }

    /**
     * Set the threshold of the configured condition.
     * @param value the new threshold value.
     */
    fun setThreshold(value: Int) {
        configuredCondition.value?.let { condition ->
            configuredCondition.value = condition.copy(threshold = value)
        }
    }

}