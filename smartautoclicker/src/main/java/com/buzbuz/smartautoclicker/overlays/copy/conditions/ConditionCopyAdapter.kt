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

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ItemConditionBinding
import com.buzbuz.smartautoclicker.databinding.ItemCopyHeaderBinding
import com.buzbuz.smartautoclicker.databinding.ItemCopySubHeaderBinding
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.EXACT
import kotlinx.coroutines.Job
import com.buzbuz.smartautoclicker.overlays.copy.conditions.ConditionCopyModel.ConditionCopyItem
import java.text.DateFormat
import java.util.Locale

/**
 * Adapter displaying all conditions in a list.
 * @param conditionClickedListener called when the user presses a condition.
 * @param bitmapProvider provides the conditions bitmaps to the items.
 */
class ConditionCopyAdapter(
    private val conditionClickedListener: (Condition) -> Unit,
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
) : ListAdapter<ConditionCopyItem, RecyclerView.ViewHolder>(ConditionDiffUtilCallback) {

    val spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int = when (getItem(position)) {
            is ConditionCopyItem.HeaderItem -> 2
            is ConditionCopyItem.SubHeaderItem -> 2
            is ConditionCopyItem.ConditionItem -> 1
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is ConditionCopyItem.HeaderItem -> R.layout.item_copy_header
        is ConditionCopyItem.SubHeaderItem -> R.layout.item_copy_sub_header
        is ConditionCopyItem.ConditionItem -> R.layout.item_condition
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        R.layout.item_copy_header -> HeaderViewHolder(
            ItemCopyHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        R.layout.item_copy_sub_header -> SubHeaderViewHolder(
            ItemCopySubHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        R.layout.item_condition -> ConditionViewHolder(
            ItemConditionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            bitmapProvider
        )

        else -> throw IllegalArgumentException("Unsupported view type !")
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HeaderViewHolder -> holder.onBind(item as ConditionCopyItem.HeaderItem)
            is SubHeaderViewHolder -> holder.onBind(item as ConditionCopyItem.SubHeaderItem)
            is ConditionViewHolder -> holder.onBindCondition(
                item as ConditionCopyItem.ConditionItem, conditionClickedListener
            )
        }

    }
}

/** DiffUtil Callback comparing two Conditions when updating the [ConditionCopyAdapter] list. */
object ConditionDiffUtilCallback : DiffUtil.ItemCallback<ConditionCopyItem>() {
    override fun areItemsTheSame(oldItem: ConditionCopyItem, newItem: ConditionCopyItem): Boolean =
        when {
            oldItem is ConditionCopyItem.HeaderItem && newItem is ConditionCopyItem.HeaderItem -> true
            oldItem is ConditionCopyItem.SubHeaderItem && newItem is ConditionCopyItem.SubHeaderItem -> true
            oldItem is ConditionCopyItem.ConditionItem && newItem is ConditionCopyItem.ConditionItem ->
                oldItem.condition.id == newItem.condition.id

            else -> false
        }

    override fun areContentsTheSame(oldItem: ConditionCopyItem, newItem: ConditionCopyItem): Boolean = oldItem == newItem
}

/**
 * View holder displaying a header in the [ActionCopyAdapter].
 * @param viewBinding the view binding for this header.
 */
class HeaderViewHolder(
    private val viewBinding: ItemCopyHeaderBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(header: ConditionCopyItem.HeaderItem) {
        viewBinding.textHeader.setText(header.title)
    }
}

/**
 * View holder displaying a sub header in the [ActionCopyAdapter].
 * @param viewBinding the view binding for this header.
 */
class SubHeaderViewHolder(
    private val viewBinding: ItemCopySubHeaderBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(header: ConditionCopyItem.SubHeaderItem) {
        viewBinding.textHeader.setText(header.title)
    }
}

/**
 * View holder displaying a condition in the [ConditionCopyAdapter].
 * @param viewBinding the view binding for this item.
 * @param bitmapProvider provides the conditions bitmap.
 */
class ConditionViewHolder(
    private val viewBinding: ItemConditionBinding,
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
) : RecyclerView.ViewHolder(viewBinding.root) {

    /** Job for the loading of the condition bitmap. Null until bound. */
    private var bitmapLoadingJob: Job? = null

    /**
     * Bind this view holder as a action item.
     *
     * @param condition the condition to be represented by this item.
     * @param conditionClickedListener listener notified upon user click on this item.
     */
    fun onBindCondition(conditionItem: ConditionCopyItem.ConditionItem, conditionClickedListener: (Condition) -> Unit) {
        val condition = conditionItem.condition
        viewBinding.apply {
            conditionName.text = condition.name

            conditionShouldBeDetected.apply {
                if (condition.shouldBeDetected) {
                    setImageResource(R.drawable.ic_confirm)
                } else {
                    setImageResource(R.drawable.ic_cancel)
                }
            }

            when (condition) {
                is Condition.Capture -> {
                    conditionImage.isVisible = true
                    conditionGroupCapture.isVisible = true
                    conditionProcessName.isVisible = false
                    conditionPeriod.isVisible = false
                    conditionV1Separator.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        endToStart = conditionDetectionType.id
                        endToEnd = -1
                    }

                    conditionThreshold.text = itemView.context.getString(
                        R.string.dialog_condition_copy_threshold,
                        condition.threshold
                    )
                    conditionDetectionType.setImageResource(
                        if (condition.detectionType == EXACT) R.drawable.ic_detect_exact else R.drawable.ic_detect_whole_screen
                    )

                    bitmapLoadingJob?.cancel()
                    bitmapLoadingJob = bitmapProvider.invoke(condition) { bitmap ->
                        if (bitmap != null) {
                            conditionImage.setImageBitmap(bitmap)
                        } else {
                            conditionImage.setImageDrawable(
                                ContextCompat.getDrawable(itemView.context, R.drawable.ic_cancel)?.apply {
                                    setTint(Color.RED)
                                }
                            )
                        }
                    }
                }

                is Condition.Process -> {
                    conditionImage.isVisible = true
                    conditionGroupCapture.isVisible = false
                    conditionProcessName.isVisible = true
                    conditionPeriod.isVisible = false
                    conditionV1Separator.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        endToStart = conditionProcessName.id
                        endToEnd = -1
                    }

                    conditionProcessName.text = condition.processName

                    bitmapLoadingJob?.cancel()
                    bitmapLoadingJob = bitmapProvider.invoke(condition) { bitmap ->
                        if (bitmap != null) {
                            conditionImage.setImageBitmap(bitmap)
                        } else {
                            conditionImage.setImageDrawable(
                                ContextCompat.getDrawable(itemView.context, R.drawable.ic_cancel)?.apply {
                                    setTint(Color.RED)
                                }
                            )
                        }
                    }
                }

                is Condition.Timer -> {
                    conditionImage.isVisible = false
                    conditionGroupCapture.isVisible = false
                    conditionProcessName.isVisible = false
                    conditionPeriod.isVisible = true
                    conditionV1Separator.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        endToStart = -1
                        endToEnd = 0
                    }

                    conditionPeriod.text = DateFormat.getTimeInstance().numberFormat.format(condition.period)
                }
            }
        }

        itemView.setOnClickListener { conditionClickedListener.invoke(condition) }
    }
}