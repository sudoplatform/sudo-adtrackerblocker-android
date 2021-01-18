/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker

import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.matchers.string.shouldStartWith
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import org.awaitility.Awaitility
import java.util.concurrent.TimeUnit

/**
 * Test functions.
 *
 * @since 2020-11-25
 */
fun checkEasyList(easyListBytes: ByteArray?) {
    easyListBytes shouldNotBe null
    easyListBytes!!.size shouldBeGreaterThan 10_000
    val easyList = String(easyListBytes)
    easyList shouldStartWith "[Adblock Plus 2.0]"
    easyList shouldContain "! Title: EasyList"
    easyList shouldContain "! Homepage: https://easylist.to/"
}

fun waitForClientInitToComplete(client: SudoAdTrackerBlockerClient) {
    Awaitility.await()
        .atLeast(100, TimeUnit.MILLISECONDS)
        .atMost(10, TimeUnit.SECONDS)
        .pollInterval(100, TimeUnit.MILLISECONDS)
        .until {
            when (client.status) {
                SudoAdTrackerBlockerClient.FilterEngineStatus.UNKNOWN,
                SudoAdTrackerBlockerClient.FilterEngineStatus.READY,
                SudoAdTrackerBlockerClient.FilterEngineStatus.ERROR -> true
                SudoAdTrackerBlockerClient.FilterEngineStatus.PREPARING -> false
            }
        }
    if (client is DefaultAdTrackerBlockerClient) {
        if (client.status == SudoAdTrackerBlockerClient.FilterEngineStatus.ERROR) {
            client.blockingProviderError?.let { throw it }
        } else {
            client.blockingProviderError shouldBe null
        }
    }
}
