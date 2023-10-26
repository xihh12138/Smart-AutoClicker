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
package com.buzbuz.smartautoclicker.overlays.utils

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView

/** [TextWatcher] implementation allowing to only declare [TextWatcher.afterTextChanged] in implementation. */
abstract class OnAfterTextChangedListener : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}

/**
 * Add a text changed listener to this TextView using the provided actions
 *
 * @return the [TextWatcher] added to the TextView
 */
inline fun TextView.addPeriodTextChangedListener(
    invokePeriod: Long,
    crossinline onTextChanged: (
        text: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) -> Unit = { _, _, _, _ -> },
): TextWatcher {
    val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            onTextChanged.invoke(text, msg.arg1, msg.arg2, msg.obj as Int)
        }
    }

    val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
            handler.removeMessages(0)

            val message = Message.obtain().apply {
                what = 0
                arg1 = start
                arg2 = before
                obj = count
            }

            handler.sendMessageDelayed(message, invokePeriod)
        }
    }
    addTextChangedListener(textWatcher)

    return textWatcher
}
