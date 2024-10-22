/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.samples

import android.content.Context
import com.sudoplatform.sudoadtrackerblocker.SudoAdTrackerBlockerClient
import com.sudoplatform.sudoadtrackerblocker.types.Ruleset
import com.sudoplatform.sudoadtrackerblocker.types.allRulesets
import com.sudoplatform.sudoadtrackerblocker.types.toHostException
import com.sudoplatform.sudoadtrackerblocker.types.toPageException
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudouser.SudoUserClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

/**
 * These are sample snippets of code that are included in the generated documentation. They are
 * placed here in the test code so that at least we know they will compile.
 */
@RunWith(RobolectricTestRunner::class)
@Suppress("UNUSED_VARIABLE")
class Samples {

    @Test
    fun mockTest() {
        // Just to keep junit happy
    }

    private val context = mock<Context>()

    fun buildClient() {
        // This is how to construct the SudoAdTrackerBlockerClient

        // Create a logger for any messages or errors
        val logger = Logger("MyApplication", AndroidUtilsLogDriver(LogLevel.INFO))

        // Create an instance of SudoUserClient to perform registration and sign in.
        val sudoUserClient = SudoUserClient.builder(context)
            .setNamespace("com.mycompany.myapplication")
            .setLogger(logger)
            .build()

        // Create an instance of SudoAdTrackerBlockerClient block advertisers and trackers
        val sudoAdTrackerBlocker = SudoAdTrackerBlockerClient.builder()
            .setContext(context)
            .setSudoUserClient(sudoUserClient)
            .setLogger(logger)
            .build()
    }

    private lateinit var client: SudoAdTrackerBlockerClient

    // This function hides the GlobalScope from the code used in the documentation. The use
    // of GlobalScope is not something that should be recommended in the code samples.
    private fun launch(
        block: suspend CoroutineScope.() -> Unit,
    ) = GlobalScope.launch { block.invoke(GlobalScope) }

    fun listRulesets() {
        launch {
            val rulesets = withContext(Dispatchers.IO) {
                client.listRulesets()
            }
            // Find the advertising blocking ruleset
            val adBlockRuleset = rulesets.find { ruleset ->
                ruleset.type == Ruleset.Type.AD_BLOCKING
            }
        }
    }

    fun updateRulesets() {
        launch {
            withContext(Dispatchers.IO) {
                // For example, if only interested in ads and privacy blocking
                client.updateRulesets(
                    Ruleset.Type.AD_BLOCKING,
                    Ruleset.Type.PRIVACY,
                )
            }
        }
    }

    fun getActiveRulesets() {
        launch {
            val activeRulesets = withContext(Dispatchers.IO) {
                client.getActiveRulesets()
            }
            println("Active rulesets: $activeRulesets")
        }
    }

    fun setActiveRulesets() {
        launch {
            withContext(Dispatchers.IO) {
                // For example, if only interested in ads and privacy blocking
                client.setActiveRulesets(
                    Ruleset.Type.AD_BLOCKING,
                    Ruleset.Type.PRIVACY,
                )
                // Or if interested in all the rulesets
                client.setActiveRulesets(allRulesets())
            }
        }
    }

    fun checkUrl() {
        launch {
            if (client.status != SudoAdTrackerBlockerClient.FilterEngineStatus.READY) {
                return@launch
            }
            withContext(Dispatchers.IO) {
                val urlStatus = client.checkUrl(
                    url = "http://somehost.com/somewhere/ad?type=banner",
                    sourceUrl = "http://somehost.com/about-us",
                )
                if (urlStatus == SudoAdTrackerBlockerClient.CheckUrlResult.BLOCKED) {
                    // URL should not be loaded
                }
            }
        }
    }

    fun addExceptions() {
        launch {
            withContext(Dispatchers.IO) {
                client.addExceptions(
                    toHostException("http://somehost.com"),
                    toPageException("http://myfavourite.domain.eu/homepage"),
                )
            }
        }
    }

    fun removeExceptions() {
        launch {
            withContext(Dispatchers.IO) {
                client.removeExceptions(
                    toHostException("http://somehost.com"),
                    toPageException("http://myfavourite.domain.eu/homepage"),
                )
            }
        }
    }

    fun removeAllExceptions() {
        launch {
            withContext(Dispatchers.IO) {
                client.removeAllExceptions()
            }
        }
    }

    fun getExceptions() {
        launch {
            val isMySiteExcepted = withContext(Dispatchers.IO) {
                client.getExceptions().firstOrNull { exc ->
                    exc.source.contains("http://myfavourite.domain.eu")
                }
            } ?: false
        }
    }
}
