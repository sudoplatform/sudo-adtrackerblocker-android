/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.blocking

import com.google.common.base.Stopwatch
import com.sudoplatform.sudoadtrackerblocker.BaseIntegrationTest
import com.sudoplatform.sudoadtrackerblocker.TestData.ADVERTISERS
import com.sudoplatform.sudoadtrackerblocker.TestData.PRIVACY_VIOLATORS
import com.sudoplatform.sudoadtrackerblocker.TestData.SHOULD_NOT_BE_BLOCKED
import com.sudoplatform.sudoadtrackerblocker.toUrl
import io.kotlintest.matchers.numerics.shouldBeLessThan
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val MAX_RULE_LOADING_MS = 1000L
private const val MAX_URL_BLOCKING_MS = 200L

/**
 * Test the operation of [DefaultBlockingProvider] on a Android device or emulator.
 */
class BlockingProviderTest : BaseIntegrationTest() {

    private val blockingProvider = DefaultBlockingProvider(logger)

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() {
        blockingProvider.close()
        Timber.uprootAll()
    }

    @Test
    fun shouldBlockPrivacyViolatorUrls() = runBlocking<Unit> {
        blockingProvider.setRules(listOf(readTextFile("easyprivacy.txt")))

        for (testCase in PRIVACY_VIOLATORS.keys) {
            blockingProvider.checkIsUrlBlocked(
                testCase.toUrl(),
                "http://somehost.eu/contact",
                "script"
            ) shouldBe true
        }
    }

    @Test
    fun shouldBlockAdvertisingUrls() = runBlocking<Unit> {
        blockingProvider.setRules(listOf(readTextFile("easylist.txt")))

        for (testCase in ADVERTISERS.keys) {
            blockingProvider.checkIsUrlBlocked(
                testCase.toUrl(),
                "http://somehost.eu/contact",
                "script"
            ) shouldBe true
        }
    }

    @Test
    fun shouldBlockAllBadUrls() = runBlocking<Unit> {
        blockingProvider.setRules(
            listOf(
                readTextFile("easylist.txt"),
                readTextFile("easyprivacy.txt")
            )
        )

        blockingProvider.checkIsUrlBlocked(
            "http://youtube.com/ptracking?",
            "http://somehost.eu/contact",
            "script"
        ) shouldBe true

        blockingProvider.checkIsUrlBlocked(
            "ad.doubleclick.net",
            "http://somehost.eu/contact",
            "script"
        ) shouldBe true

        for (testCase in ADVERTISERS.keys + PRIVACY_VIOLATORS.keys) {
            blockingProvider.checkIsUrlBlocked(
                testCase.toUrl(),
                "http://somehost.eu/contact",
                "script"
            ) shouldBe true
        }
    }

    @Test
    fun shouldNotBlockGoodUrls() = runBlocking<Unit> {
        blockingProvider.setRules(
            listOf(
                readTextFile("easylist.txt"),
                readTextFile("easyprivacy.txt")
            )
        )

        for (testCase in SHOULD_NOT_BE_BLOCKED.keys) {
            blockingProvider.checkIsUrlBlocked(
                testCase.toUrl(),
                "http://somehost.eu/contact",
                "script"
            ) shouldBe false
        }
    }

    @Test
    fun timingTest() = runBlocking<Unit> {
        val stopwatch = Stopwatch.createStarted()
        blockingProvider.setRules(
            listOf(
                readTextFile("easylist.txt"),
                readTextFile("easyprivacy.txt")
            )
        )
        stopwatch.stop()
        println("Rules loading took $stopwatch")
        stopwatch.elapsed(TimeUnit.MILLISECONDS) shouldBeLessThan MAX_RULE_LOADING_MS

        stopwatch.reset()
        stopwatch.start()

        val allTestCases = PRIVACY_VIOLATORS.keys + ADVERTISERS.keys + SHOULD_NOT_BE_BLOCKED.keys
        for (testCase in allTestCases) {
            blockingProvider.checkIsUrlBlocked(
                testCase.toUrl(),
                "http://somehost.eu/contact",
                "script"
            )
        }

        stopwatch.stop()
        println("Testing ${allTestCases.size} URLs took $stopwatch")
        stopwatch.elapsed(TimeUnit.MILLISECONDS) shouldBeLessThan MAX_URL_BLOCKING_MS
    }
}
