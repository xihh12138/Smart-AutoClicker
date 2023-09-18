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
package com.buzbuz.smartautoclicker.activity

import android.content.Intent
import android.os.Bundle

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Entry point activity for the application.
 * Shown when the user clicks on the launcher icon for the application, this activity will displays the list of
 * available scenarios, if any.
 */
class ScenarioActivity : AppCompatActivity() {

    /** ViewModel providing the click scenarios data to the UI. */
    private val scenarioViewModel: ScenarioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scenario)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.title = resources.getString(R.string.activity_scenario_title)
//        scenarioViewModel.stopScenario()

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        intent.getStringExtra(EXTRA_IN_LOAD_SCENARIO_NAME)?.let { name ->
            scenarioViewModel.viewModelScope.launch {
                withTimeoutOrNull(2000) {
                    scenarioViewModel.scenarioList.takeWhile { it.isNotEmpty() }.first()
                }?.find { it.scenario.name == name }?.let {
                    scenarioViewModel.loadScenario(it.scenario)
                }
            }
        }
    }

    companion object {
        const val EXTRA_IN_LOAD_SCENARIO_NAME = "LOAD_SCENARIO_NAME"
    }
}
