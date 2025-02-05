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
package com.buzbuz.smartautoclicker.overlays.eventconfig

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ItemConditionCardBinding
import com.buzbuz.smartautoclicker.databinding.ItemNewCopyCardBinding
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.EXACT
import com.buzbuz.smartautoclicker.extensions.setLeftCompoundDrawable
import com.buzbuz.smartautoclicker.extensions.toFormatHmsString
import com.buzbuz.smartautoclicker.overlays.utils.setIconTint
import kotlinx.coroutines.Job
import java.text.DateFormat
import java.util.Collections

/**
 * Adapter displaying the conditions for the event displayed by the dialog.
 * Also provide a item displayed in the last position to add a new condition.
 *
 * @param addConditionClickedListener the listener called when the user clicks on the add item. True if this is the
 *                                    first item, false if not.
 * @param conditionClickedListener the listener called when the user clicks on a condition.
 * @param bitmapProvider provides the conditions bitmaps to the items.
 */
class ConditionAdapter(
    private val addConditionClickedListener: () -> Unit,
    private val copyConditionClickedListener: () -> Unit,
    private val conditionClickedListener: (Int, Condition) -> Unit,
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val conditionReorderListener: (List<ConditionListItem>) -> Unit,
) : ListAdapter<ConditionListItem, RecyclerView.ViewHolder>(ConditionDiffUtilCallback) {

    /** The list of actions to be shown by this adapter.*/
    private var conditions: MutableList<ConditionListItem>? = null

    override fun submitList(list: MutableList<ConditionListItem>?) {
        conditions = list
        super.submitList(list)
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is ConditionListItem.AddConditionItem -> R.layout.item_new_copy_card
            is ConditionListItem.ConditionItem -> R.layout.item_condition_card
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_new_copy_card -> AddConditionViewHolder(
                ItemNewCopyCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                addConditionClickedListener,
                copyConditionClickedListener,
            )

            R.layout.item_condition_card -> ConditionViewHolder(
                ItemConditionCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                bitmapProvider,
            )

            else -> throw IllegalArgumentException("Unsupported view type !")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ConditionViewHolder -> holder.onBindCondition(
                ((getItem(position) as ConditionListItem.ConditionItem).condition),
                conditionClickedListener,
            )

            is AddConditionViewHolder -> holder.onBind((getItem(position) as ConditionListItem.AddConditionItem))
        }
    }

    /**
     * Swap the position of two actions in the list.
     *
     * @param from the position of the click to be moved.
     * @param to the new position of the click to be moved.
     */
    fun swapActions(from: Int, to: Int) {
        conditions?.let {
            Collections.swap(it, from, to)
            notifyItemMoved(from, to)
        }
    }

    /** Notify for an item drag and drop completion. */
    fun notifyMoveFinished() {
        conditionReorderListener(currentList)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is ConditionViewHolder) {
            holder.onUnbind()
        }
        super.onViewRecycled(holder)
    }
}

/** DiffUtil Callback comparing two ActionItem when updating the [ConditionAdapter] list. */
object ConditionDiffUtilCallback : DiffUtil.ItemCallback<ConditionListItem>() {
    override fun areItemsTheSame(oldItem: ConditionListItem, newItem: ConditionListItem): Boolean = when {
        oldItem is ConditionListItem.AddConditionItem && newItem is ConditionListItem.AddConditionItem -> true
        oldItem is ConditionListItem.ConditionItem && newItem is ConditionListItem.ConditionItem ->
            oldItem.condition.id == oldItem.condition.id

        else -> false
    }

    override fun areContentsTheSame(oldItem: ConditionListItem, newItem: ConditionListItem): Boolean =
        oldItem == newItem
}

/** View holder for the add condition item. */
class AddConditionViewHolder(
    private val viewBinding: ItemNewCopyCardBinding,
    addActionClickedListener: () -> Unit,
    copyActionClickedListener: () -> Unit
) : RecyclerView.ViewHolder(viewBinding.root) {

    init {
        viewBinding.newItem.setOnClickListener { addActionClickedListener() }
        viewBinding.copyItem.setOnClickListener { copyActionClickedListener() }
    }

    fun onBind(action: ConditionListItem.AddConditionItem) {
        viewBinding.copyItem.visibility =
            if (action.shouldDisplayCopy) View.VISIBLE
            else View.GONE
        viewBinding.separator.visibility =
            if (action.shouldDisplayCopy) View.VISIBLE
            else View.GONE
    }
}

/**
 * View holder displaying a condition in the [ConditionAdapter].
 * @param viewBinding the view binding for this item.
 * @param bitmapProvider provides the conditions bitmaps to the items.
 */
class ConditionViewHolder(
    private val viewBinding: ItemConditionCardBinding,
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?
) : RecyclerView.ViewHolder(viewBinding.root) {

    /** Job for the loading of the condition bitmap. Null until bound. */
    private var bitmapLoadingJob: Job? = null

    /**
     * Bind this view holder as a condition item.
     *
     * @param condition the condition to be represented by this item.
     * @param conditionClickedListener listener notified upon user click on this item.
     */
    fun onBindCondition(condition: Condition, conditionClickedListener: (Int, Condition) -> Unit) {
        viewBinding.imageCondition.scaleType = ImageView.ScaleType.FIT_CENTER
        itemView.setOnClickListener { conditionClickedListener.invoke(bindingAdapterPosition, condition) }

        when (condition) {
            is Condition.Capture -> {
                viewBinding.conditionGroupCapture.isVisible = true
                viewBinding.conditionProcessName.isVisible = false
                viewBinding.imageCondition.isVisible = true
                viewBinding.conditionBackground.isVisible = true
                viewBinding.conditionPeriod.isVisible = false

                if (condition.shouldBeDetected) {
                    viewBinding.conditionShouldBeDetected.apply {
                        setImageResource(R.drawable.ic_confirm)
                        setIconTint(R.color.overlayMenuButtons)
                    }
                } else {
                    viewBinding.conditionShouldBeDetected.apply {
                        setImageResource(R.drawable.ic_cancel)
                        setIconTint(R.color.overlayMenuButtons)
                    }
                }

                viewBinding.conditionDetectionType.setImageResource(
                    if (condition.detectionType == EXACT) R.drawable.ic_detect_exact else R.drawable.ic_detect_whole_screen
                )
                viewBinding.conditionDetectionType.setIconTint(R.color.overlayMenuButtons)

                bitmapLoadingJob?.cancel()
                bitmapLoadingJob = bitmapProvider.invoke(condition) { bitmap ->
                    if (bitmap != null) {
                        viewBinding.imageCondition.setImageBitmap(bitmap)
                    } else {
                        viewBinding.imageCondition.setImageDrawable(
                            ContextCompat.getDrawable(itemView.context, R.drawable.ic_cancel)?.apply {
                                setTint(Color.RED)
                            }
                        )
                    }
                }
            }

            is Condition.Process -> {
                viewBinding.conditionGroupCapture.isVisible = false
                viewBinding.conditionProcessName.isVisible = true
                viewBinding.imageCondition.isVisible = true
                viewBinding.conditionBackground.isVisible = true
                viewBinding.conditionPeriod.isVisible = false

                viewBinding.conditionProcessName.text = condition.processName

                if (condition.shouldBeDetected) {
                    viewBinding.conditionProcessName.setLeftCompoundDrawable(
                        R.drawable.ic_confirm,
                        ContextCompat.getColor(viewBinding.conditionProcessName.context, R.color.overlayMenuButtons)
                    )
                } else {
                    viewBinding.conditionProcessName.setLeftCompoundDrawable(
                        R.drawable.ic_cancel,
                        ContextCompat.getColor(viewBinding.conditionProcessName.context, R.color.overlayMenuButtons)
                    )
                }

                bitmapLoadingJob?.cancel()
                bitmapLoadingJob = bitmapProvider.invoke(condition) { bitmap ->
                    if (bitmap != null) {
                        viewBinding.imageCondition.setImageBitmap(bitmap)
                    } else {
                        viewBinding.imageCondition.setImageDrawable(
                            ContextCompat.getDrawable(itemView.context, R.drawable.ic_cancel)?.apply {
                                setTint(Color.RED)
                            }
                        )
                    }
                }
            }

            is Condition.Timer -> {
                viewBinding.conditionGroupCapture.isVisible = false
                viewBinding.conditionProcessName.isVisible = false
                viewBinding.imageCondition.isVisible = false
                viewBinding.conditionBackground.isVisible = false
                viewBinding.conditionPeriod.isVisible = true

                viewBinding.conditionPeriod.text = condition.period.toFormatHmsString()
            }
        }
    }

    /** Unbind this view holder for a previously bound data model. */
    fun onUnbind() {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = null
    }
}

/** ItemTouchHelper attached to the adapter */
class ConditionTouchHelper : ItemTouchHelper.SimpleCallback(0, 0) {

    /** Tells if the user is currently dragging an item. */
    private var isDragging: Boolean = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        isDragging = true
        (recyclerView.adapter as ConditionAdapter).swapActions(
            viewHolder.bindingAdapterPosition,
            target.bindingAdapterPosition
        )
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (isDragging) {
            (recyclerView.adapter as ConditionAdapter).notifyMoveFinished()
            isDragging = false
        }
    }

    override fun canDropOver(
        recyclerView: RecyclerView,
        current: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        if (target is AddConditionViewHolder) return false
        return true
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = if (viewHolder.bindingAdapterPosition == recyclerView.adapter!!.itemCount - 1) 0
        else ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { /* Nothing do to */
    }
}