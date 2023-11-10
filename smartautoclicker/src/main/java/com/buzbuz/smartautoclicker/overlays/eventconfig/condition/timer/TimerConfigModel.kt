package com.buzbuz.smartautoclicker.overlays.eventconfig.condition.timer

import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.overlays.eventconfig.condition.ConditionModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class TimerConfigModel(
    private val viewModelScope: CoroutineScope,
    private val configuredCondition: MutableStateFlow<Condition.Timer?>,
) : ConditionModel() {

    override val isValidCondition: Flow<Boolean> = configuredCondition.map { condition ->
        condition != null && condition.name.isNotEmpty() && condition.period != 0L
    }

    val period = configuredCondition.mapNotNull { it?.period }

    fun setPeriod(period: Long) {
        configuredCondition.value?.let { condition ->
            configuredCondition.value = condition.copy(period = period)
        }
    }
}