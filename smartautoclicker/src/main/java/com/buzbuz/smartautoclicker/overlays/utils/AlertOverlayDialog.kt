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
package com.buzbuz.smartautoclicker.overlays.utils

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.databinding.DialogDebugConfigBinding

import kotlinx.coroutines.launch

class AlertOverlayDialog(
    context: Context,
    private val title: Int,
    private val message: String,
    private val onPositive: AlertOverlayDialog.() -> Unit,
    private val onNegative: (AlertOverlayDialog.() -> Unit)? = null
) : OverlayDialogController(context) {

    override fun onCreateDialog(): AlertDialog.Builder {
        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                onPositive()
            }
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface, _: Int ->
                if (onNegative == null) {
                    dismiss()
                } else {
                    onNegative.invoke(this)
                }
            }
    }

    override fun onDialogCreated(dialog: AlertDialog) {

    }
}