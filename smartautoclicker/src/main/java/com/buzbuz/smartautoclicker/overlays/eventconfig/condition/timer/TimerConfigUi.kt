package com.buzbuz.smartautoclicker.overlays.eventconfig.condition.timer

import android.text.Editable
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.databinding.DialogConditionConfigBinding
import com.buzbuz.smartautoclicker.overlays.utils.OnAfterTextChangedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


fun DialogConditionConfigBinding.setupCaptureUi(
    lifecycleOwner: LifecycleOwner,
    lifecycleScope: CoroutineScope,
    viewModel: TimerConfigModel,
) {
    val context = root.context

    etConditionPeriod.isVisible = true

    etConditionPeriod.apply {
        setSelectAllOnFocus(true)
        addTextChangedListener(object : OnAfterTextChangedListener() {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setPeriod(s?.toString()?.toLongOrNull() ?: 0)
            }
        })
    }

    lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                viewModel.period.collect { period ->
                    etConditionPeriod.setText(period.toString())
                    etConditionPeriod.setSelection(etConditionPeriod.text.length)
                }
            }
        }
    }

}