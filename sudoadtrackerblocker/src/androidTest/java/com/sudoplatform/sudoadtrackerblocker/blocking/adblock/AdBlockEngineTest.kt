/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.blocking.adblock

import com.sudoplatform.sudoadtrackerblocker.BaseIntegrationTest
import com.sudoplatform.sudoadtrackerblocker.FilterEngine
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

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() {
        Timber.uprootAll()
    }

    @Test
    fun shouldBlockPrivacyViolatorUrls() = runBlocking<Unit> {
        val adBlockEngine = FilterEngine(listOf(readTextFile("easyprivacy.txt")))

        PRIVACY_VIOLATORS.forEach { testCase, _ ->
            adBlockEngine.checkNetworkUrlsMatched(
                testCase.toUrl(),
                "http://somehost.eu/contact",
                "script",
            ) shouldBe true
        }
    }

    @Test
    fun shouldBlockAdvertisingUrls() {
        val adBlockEngine = FilterEngine(listOf(readTextFile("easylist.txt")))
        ADVERTISERS.forEach { testCase, _ ->
            adBlockEngine.checkNetworkUrlsMatched(
                testCase.toUrl(),
                "http://somehost.eu/contact",
                "script",
            ) shouldBe true
        }
    }

    @Test
    fun shouldNotBlockGoodUrls() {
        val adBlockEngine = FilterEngine(listOf(readTextFile("easylist.txt"), readTextFile("easyprivacy.txt")))
        SHOULD_NOT_BE_BLOCKED.forEach { testCase, _ ->
            adBlockEngine.checkNetworkUrlsMatched(
                testCase.toUrl(),
                "http://somehost.eu/contact",
                "script",
            ) shouldBe false
        }
    }

    @Test
    fun shouldBlockCorrectly() = runBlocking<Unit> {
        val easyList = readTextFile("easylist.txt")
        val easyPrivacy = readTextFile("easyprivacy.txt")

        val filterEngine = FilterEngine(listOf(easyList, easyPrivacy))
        val currentUrl = "http://somehost.eu/contact"

        filterEngine.checkNetworkUrlsMatched("http://ad.doubleclick.net", currentUrl, "script") shouldBe true
        filterEngine.checkNetworkUrlsMatched("ad.doubleclick.net".toUrl(), currentUrl, "script") shouldBe true
        filterEngine.checkNetworkUrlsMatched("http://youtube.com/ptracking?", currentUrl, "script") shouldBe true

        filterEngine.checkNetworkUrlsMatched("http://shoesandcoats.com", currentUrl, "script") shouldBe false
        filterEngine.checkNetworkUrlsMatched("http://www.anonyome.com", currentUrl, "script") shouldBe false
        filterEngine.checkNetworkUrlsMatched("http://www.anonyome.com/about.js", currentUrl, "script") shouldBe false
        filterEngine.checkNetworkUrlsMatched("http://www.mysudo.com/", currentUrl, "script") shouldBe false
        filterEngine.checkNetworkUrlsMatched("http://www.brisbanetimes.com/", currentUrl, "script") shouldBe false
    }
}
