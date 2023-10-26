package com.buzbuz.smartautoclicker.overlays.eventconfig.condition.process

import android.content.ComponentName
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.DETECT_AREA
import com.buzbuz.smartautoclicker.domain.EXACT
import com.buzbuz.smartautoclicker.overlays.eventconfig.condition.ConditionModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class ProcessConfigModel(
    private val viewModelScope: CoroutineScope,
    private val configuredCondition: MutableStateFlow<Condition.Process?>,
) : ConditionModel() {

    override val isValidCondition: Flow<Boolean> = configuredCondition.map { condition ->
        condition != null && condition.name.isNotEmpty() && condition.processName.isNotEmpty()
    }

    val processName = configuredCondition.mapNotNull { it?.processName }

    fun setProcessName(componentName: String) {
        configuredCondition.value?.let { condition ->
            configuredCondition.value = condition.copy(
                processName = componentName,
            )
        }
    }
}