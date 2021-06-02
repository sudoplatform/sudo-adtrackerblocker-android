/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudoadtrackerblocker

import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import com.sudoplatform.sudoadtrackerblocker.types.Ruleset
import com.sudoplatform.sudoadtrackerblocker.types.allRulesets
import com.sudoplatform.sudoadtrackerblocker.types.noRulesets
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoAdTrackerBlockerClient.setActiveRulesets] using mocks and spies.
 *
 * @since 2020-11-30
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoAdTrackerBlockerClientSetActiveRulesetsTest : BaseTests() {

    private val allDisabledPreferences = emptyMap<String, String>()
    private val allEnabledPreferences: Map<String, String>

    init {
        val allEnabledPreference = mutableMapOf<String, String>()
        allRulesets().forEach { rulesetType ->
            allEnabledPreference[rulesetType.name] = true.toString()
        }
        allEnabledPreferences = allEnabledPreference
    }

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
    fun `setActiveRulesets() with no args should call storage provider`() = runBlocking<Unit> {

        adTrackerBlockerClient.setActiveRulesets()

        verify(mockStorageProvider).writePreferences(allDisabledPreferences)
        verify(mockBlockingProvider, times(2)).close()
    }

    @Test
    fun `setActiveRulesets() with no rulesets should call storage provider`() = runBlocking<Unit> {

        adTrackerBlockerClient.setActiveRulesets(noRulesets())

        verify(mockStorageProvider).writePreferences(allDisabledPreferences)
        verify(mockBlockingProvider, times(2)).close()
    }

    @Test
    fun `setActiveRulesets() with all rulesets should call storage provider`() = runBlocking<Unit> {

        adTrackerBlockerClient.setActiveRulesets(allRulesets())

        verify(mockStorageProvider).writePreferences(allEnabledPreferences)
        verify(mockBlockingProvider, times(2)).close()
    }

    @Test
    fun `setActiveRulesets() with one ruleset should call storage provider`() = runBlocking<Unit> {

        adTrackerBlockerClient.setActiveRulesets(Ruleset.Type.AD_BLOCKING)

        verify(mockStorageProvider).writePreferences(mapOf(Ruleset.Type.AD_BLOCKING.name to "true"))
        verify(mockBlockingProvider, times(2)).close()
    }

    @Test
    fun `setActiveRulesets() should throw when storage provider throws`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { writePreferences(any()) } doThrow IOException("mock")
        }

        shouldThrow<SudoAdTrackerBlockerException.FailedException> {
            adTrackerBlockerClient.setActiveRulesets()
        }

        verify(mockStorageProvider).writePreferences(any())
    }

    @Test
    fun `setActiveRulesets() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { writePreferences(any()) } doThrow CancellationException("Mock")
        }

        shouldThrow<CancellationException> {
            adTrackerBlockerClient.setActiveRulesets()
        }

        verify(mockStorageProvider).writePreferences(any())
    }
}
