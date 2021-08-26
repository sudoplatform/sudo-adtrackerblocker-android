/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudoadtrackerblocker

import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import com.sudoplatform.sudoadtrackerblocker.types.Ruleset
import com.sudoplatform.sudoadtrackerblocker.types.allRulesets
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoAdTrackerBlockerClient.getActiveRulesets] using mocks and spies.
 *
 * @since 2020-11-30
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoAdTrackerBlockerClientGetActiveRulesetsTest : BaseTests() {

    private val allDisabledStatus = emptyArray<Ruleset.Type>()
    private val allEnabledStatus = allRulesets()

    @After
    fun fini() {
        verifyMocksUsedInClientInit()
        verifyNoMoreInteractions(
            mockContext,
            mockUserClient,
            mockS3Client,
            mockStorageProvider,
            mockBlockingProvider
        )
        runBlocking {
            adTrackerBlockerClient.clearStorage()
        }
    }

    @Test
    fun `getActiveRulesets() should return all disabled when storage provider returns empty preferences`() = runBlocking<Unit> {

        val status = adTrackerBlockerClient.getActiveRulesets()
        status shouldNotBe null
        status shouldBe allDisabledStatus

        verify(mockStorageProvider, atLeastOnce()).readPreferences()
    }

    @Test
    fun `getActiveRulesets() should return all disabled when storage provider returns bogus preferences`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { readPreferences() } doReturn mapOf(Ruleset.Type.UNKNOWN.name to "true")
        }

        val status = adTrackerBlockerClient.getActiveRulesets()
        status shouldNotBe null
        status shouldBe allDisabledStatus

        verify(mockStorageProvider, atLeastOnce()).readPreferences()
    }

    @Test
    fun `getActiveRulesets() should return all enabled when storage provider returns extras preferences`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { readPreferences() } doReturn mapOf(
                Ruleset.Type.AD_BLOCKING.name to "true",
                Ruleset.Type.PRIVACY.name to "true",
                Ruleset.Type.SOCIAL.name to "true",
                Ruleset.Type.UNKNOWN.name to "false"
            )
        }

        val status = adTrackerBlockerClient.getActiveRulesets()
        status shouldNotBe null
        status shouldBe allEnabledStatus

        verify(mockStorageProvider, atLeastOnce()).readPreferences()
    }

    @Test
    fun `getActiveRulesets() should throw when storage provider throws`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { readPreferences() } doThrow IOException("mock")
        }

        shouldThrow<SudoAdTrackerBlockerException.FailedException> {
            adTrackerBlockerClient.getActiveRulesets()
        }

        verify(mockStorageProvider, atLeastOnce()).readPreferences()
    }

    @Test
    fun `getActiveRulesets() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { readPreferences() } doThrow CancellationException("Mock")
        }

        shouldThrow<CancellationException> {
            adTrackerBlockerClient.getActiveRulesets()
        }

        verify(mockStorageProvider, atLeastOnce()).readPreferences()
    }

    @Test
    fun `ENTITLEMENT_NAME should not be null and should have the correct value`() = runBlocking {
        val entitlementName: String = adTrackerBlockerClient.ENTITLEMENT_NAME
        entitlementName shouldNotBe null
        entitlementName shouldBe "sudoplatform.atb.atbUserEntitled"
    }
}
