/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.transformers

import com.sudoplatform.sudoadtrackerblocker.types.toHostException
import com.sudoplatform.sudoadtrackerblocker.types.toPageException
import io.kotlintest.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test the operation of [ExceptionsTransformer].
 */
@RunWith(RobolectricTestRunner::class)
internal class ExceptionsTransformerTest {

    @Test
    fun `transformer should convert host only URLs to rules`() {
        val testCases = mapOf(
            "foo.com" to "|http://foo.com^\n|https://foo.com^\n",
            "foo.bar.com" to "|http://foo.bar.com^\n|https://foo.bar.com^\n",
            "http://foo.com" to "|http://foo.com^\n|https://foo.com^\n",
            "https://foo.com" to "|http://foo.com^\n|https://foo.com^\n",
        )
        for ((url, result) in testCases) {
            val exc = toHostException(url)
            val rules = String(ExceptionsTransformer.toExceptionRules(setOf(exc))!!)
            rules shouldBe result
        }
    }

    @Test
    fun `transformer should convert host and path URLs to rules`() {
        val testCases = mapOf(
            "foo.com/about-us" to "|http://foo.com/about-us^\n|https://foo.com/about-us^\n",
            "foo.bar.com/contact/external?type=phone" to "|http://foo.bar.com/contact/external^\n|https://foo.bar.com/contact/external^\n",
            "http://foo.com/about-us" to "|http://foo.com/about-us^\n|https://foo.com/about-us^\n",
            "https://foo.bar.com/contact/external?type=phone"
                to "|http://foo.bar.com/contact/external^\n|https://foo.bar.com/contact/external^\n",
        )
        for ((url, result) in testCases) {
            val exc = toPageException(url)
            val rules = String(ExceptionsTransformer.toExceptionRules(setOf(exc))!!)
            rules shouldBe result
        }
    }
}
