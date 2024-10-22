/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.types

import com.sudoplatform.sudoadtrackerblocker.SudoAdTrackerBlockerException
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test the correction operation of the [BlockingException] factory methods.
 */
@RunWith(RobolectricTestRunner::class)
class BlockingExceptionTest {

    @Test
    fun `url with host only should create host exception`() {
        val testCases = mapOf(
            "example.com" to "example.com",
            " example.com " to "example.com",
            "example.com/foo" to "example.com",
            "http://example.com/foo/bar?query=text" to "example.com",
            "https://example.com/foo/bar?query=text" to "example.com",
            "google-analytics.com/analytics.js" to "google-analytics.com",
        )
        for ((testCase, expectedSource) in testCases) {
            with(toHostException(testCase)) {
                type shouldBe BlockingException.Type.HOST
                source shouldBe expectedSource
            }
        }
    }

    @Test
    fun `should throw if url without host supplied`() {
        val testCases = arrayOf(
            "",
            "/bar/foo",
            " /bar/foo ",
            " ?&^^%*^^(*&&*(^*^%^$%#^%&(*)*)",
        )
        for (testCase in testCases) {
            shouldThrow<SudoAdTrackerBlockerException.UrlFormatException> {
                toHostException(testCase)
            }
            shouldThrow<SudoAdTrackerBlockerException.UrlFormatException> {
                toPageException(testCase)
            }
        }
    }

    @Test
    fun `url with path should create page exception`() {
        val testCases = mapOf(
            "example.com/about" to "example.com/about",
            " example.com/about " to "example.com/about",
            " example.com/about/ " to "example.com/about",
            "example.com/foo" to "example.com/foo",
            "http://example.com/foo/bar?query=text" to "example.com/foo/bar",
            "https://example.com/foo/bar?query=text" to "example.com/foo/bar",
            "https://example.com/foo/bar/?query=text" to "example.com/foo/bar",
            "google-analytics.com/analytics.js" to "google-analytics.com/analytics.js",
        )
        for ((testCase, expectedSource) in testCases) {
            with(toPageException(testCase)) {
                type shouldBe BlockingException.Type.PAGE
                source shouldBe expectedSource
            }
        }
    }
}
