/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.blocking.adblock

import com.sudoplatform.sudoadtrackerblocker.BaseIntegrationTest
import com.sudoplatform.sudoadtrackerblocker.TestData.ADVERTISERS
import com.sudoplatform.sudoadtrackerblocker.TestData.PRIVACY_VIOLATORS
import com.sudoplatform.sudoadtrackerblocker.TestData.SHOULD_NOT_BE_BLOCKED
import com.sudoplatform.sudoadtrackerblocker.toUrl
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import timber.log.Timber

/**
 * Test the operation of [AdBlockEngine] on a Android device or emulator.
 */
class AdBlockEngineTest : BaseIntegrationTest() {

    private val adBlockEngine = AdBlockEngine()

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() {
        adBlockEngine.clearRules()
        Timber.uprootAll()
    }

    @Test
    fun shouldBlockPrivacyViolatorUrls() = runBlocking<Unit> {
        adBlockEngine.loadRules(readTextFile("easyprivacy.txt"))

        PRIVACY_VIOLATORS.forEach { testCase, requestHost ->
            adBlockEngine.shouldLoad(
                testCase.toUrl(),
                "http://somehost.eu/contact",
                "script",
                requestHost,
                "somehost.eu"
            ) shouldBe false
        }
    }

    @Test
    fun shouldBlockAdvertisingUrls() {
        adBlockEngine.loadRules(readTextFile("easylist.txt"))

        ADVERTISERS.forEach { testCase, requestHost ->
            adBlockEngine.shouldLoad(
                testCase.toUrl(),
                "http://somehost.eu/contact",
                null,
                requestHost,
                "somehost.eu"
            ) shouldBe false
        }
    }

    @Test
    fun shouldNotBlockGoodUrls() {

        adBlockEngine.loadRules(readTextFile("easyprivacy.txt"))
        adBlockEngine.loadRules(readTextFile("easylist.txt"))

        SHOULD_NOT_BE_BLOCKED.forEach { testCase, requestHost ->
            adBlockEngine.shouldLoad(
                testCase.toUrl(),
                "http://somehost.eu/contact",
                null,
                requestHost,
                "somehost.eu"
            ) shouldBe true
        }
    }

    @Test
    fun shouldResolveDomains() {
        (PRIVACY_VIOLATORS + ADVERTISERS + SHOULD_NOT_BE_BLOCKED).forEach { urlString, host ->
            adBlockEngine.domainResolver(urlString) shouldBe host
        }
    }
}
