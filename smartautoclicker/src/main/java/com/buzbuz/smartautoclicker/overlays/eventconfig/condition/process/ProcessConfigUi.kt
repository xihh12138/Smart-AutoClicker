package com.buzbuz.smartautoclicker.overlays.eventconfig.condition.process

import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.baseui.OverlayController
import com.buzbuz.smartautoclicker.databinding.DialogConditionConfigBinding
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.intent.ActivitySelectionDialog
import com.buzbuz.smartautoclicker.overlays.utils.addPeriodTextChangedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


fun DialogConditionConfigBinding.setupCaptureUi(
    lifecycleOwner: LifecycleOwner,
    lifecycleScope: CoroutineScope,
    viewModel: ProcessConfigModel,
    condition: Condition.Process,
    showSubOverlay: (OverlayController, Boolean) -> Unit
) {
    val context = root.context

    layoutConditionProcess.isVisible = true

    ivSelectProcessName.setOnClickListener {
        showSubOverlay(
            ActivitySelectionDialog(
                context = context,
                onApplicationSelected = { componentName ->
                    viewModel.setProcessName(componentName.packageName)
                }
            ),
            false,
        )
    }

    editProcessName.addPeriodTextChangedListener(100) { text, _, _, _ ->
        text?.toString()?.let { viewModel.setProcessName(it) }
    }

    lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                viewModel.processName.collect { processName ->
                    editProcessName.setText(processName)

                    kotlin.runCatching { context.packageManager.getApplicationIcon(processName) }.getOrNull()?.let {
                        imageCondition.setImageDrawable(it)
                    }
                }
            }
        }
    }

}