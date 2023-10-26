package com.buzbuz.smartautoclicker.overlays.eventconfig.condition.capture

import android.util.Log
import android.util.Size
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.OverlayController
import com.buzbuz.smartautoclicker.baseui.ScreenMetrics
import com.buzbuz.smartautoclicker.databinding.DialogConditionConfigBinding
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.DETECT_AREA
import com.buzbuz.smartautoclicker.domain.EXACT
import com.buzbuz.smartautoclicker.domain.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.extensions.setLeftRightCompoundDrawables
import com.buzbuz.smartautoclicker.overlays.eventconfig.condition.ConditionSelectorMenu
import com.buzbuz.smartautoclicker.overlays.eventconfig.condition.MAX_THRESHOLD
import com.buzbuz.smartautoclicker.overlays.utils.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


fun DialogConditionConfigBinding.setupCaptureUi(
    lifecycleOwner: LifecycleOwner,
    lifecycleScope: CoroutineScope,
    viewModel: CaptureConfigModel,
    condition: Condition.Capture,
    showSubOverlay: (OverlayController, Boolean) -> Unit
) {
    val context = root.context

    layoutConditionCapture.isVisible = true
    cardConditionThreshold.isVisible = true

    conditionDetectionType.setOnClickListener {
        viewModel.toggleDetectionType()
    }

    conditionDetectionTypeValue.setOnClickListener {
        showSubOverlay(
            ConditionSelectorMenu(
                context = context,
                onConditionSelected = { area, _ ->
                    viewModel.setDetectionArea(area)
                },
                ScreenMetrics(context).screenSize.let { size ->
                    Size(size.x, 0)
                }
            ), true
        )
    }

    seekbarDiffThreshold.apply {
        max = MAX_THRESHOLD
        progress = condition.threshold
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.setThreshold(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                viewModel.threshold.collect { threshold ->
                    textDiffThreshold.text = context.getString(
                        R.string.dialog_condition_threshold_value,
                        threshold
                    )
                }
            }

            launch {
                viewModel.detectionTypeAndValue.collect { detectionTypeAndValue ->
                    val (conditionType, detectArea) = detectionTypeAndValue
                    val tvType = conditionDetectionType
                    val tvTypeValue = conditionDetectionTypeValue
                    when (conditionType) {
                        EXACT -> {
                            tvType.text = context.getString(
                                R.string.dialog_condition_at,
                                condition.area.left,
                                condition.area.top,
                                condition.area.right,
                                condition.area.bottom
                            )
                            tvType.setLeftRightCompoundDrawables(
                                R.drawable.ic_detect_exact, R.drawable.ic_chevron
                            )

                            tvTypeValue.isVisible = false
                        }

                        WHOLE_SCREEN -> {
                            tvType.text = context.getString(R.string.dialog_condition_type_whole_screen)
                            tvType.setLeftRightCompoundDrawables(
                                R.drawable.ic_detect_whole_screen, R.drawable.ic_chevron
                            )

                            tvTypeValue.isVisible = false
                        }

                        DETECT_AREA -> {
                            tvType.text = context.getString(R.string.dialog_condition_type_detect_area)
                            tvType.setLeftRightCompoundDrawables(
                                R.drawable.ic_detect_exact, R.drawable.ic_chevron
                            )

                            tvTypeValue.isVisible = true
                            tvTypeValue.text = context.getString(
                                R.string.dialog_condition_in,
                                detectArea.left,
                                detectArea.top,
                                detectArea.right,
                                detectArea.bottom
                            )
                        }

                        else -> {
                            Log.e(TAG, "Invalid condition detection type, displaying nothing.")
                        }
                    }
                }
            }
        }
    }
}