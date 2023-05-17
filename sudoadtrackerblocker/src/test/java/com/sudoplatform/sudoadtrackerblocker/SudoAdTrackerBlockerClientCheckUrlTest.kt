/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker

import org.mockito.kotlin.doThrow
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoAdTrackerBlockerClient.checkUrl] using mocks and spies.
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoAdTrackerBlockerClientCheckUrlTest : BaseTests() {

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
    fun `checkUrl() should call blocking provider`() = runBlocking<Unit> {

        adTrackerBlockerClient.checkUrl("a", "b", "c")

        verify(mockBlockingProvider).checkIsUrlBlocked(anyString(), anyString(), anyString())
    }

    @Test
    fun `checkUrl() should throw when blocking provider not ready`() = runBlocking<Unit> {

        adTrackerBlockerClient.actualStatus = SudoAdTrackerBlockerClient.FilterEngineStatus.PREPARING

        shouldThrow<SudoAdTrackerBlockerException.FilterEngineNotReadyException> {
            adTrackerBlockerClient.checkUrl("a", "b", "c")
        }

        verify(mockStorageProvider).readPreferences()
        verify(mockBlockingProvider).close()
    }

    @Test
    fun `checkUrl() should throw when blocking provider throws`() = runBlocking<Unit> {

        mockBlockingProvider.stub {
            onBlocking {
                checkIsUrlBlocked(anyString(), anyString(), anyString())
            } doThrow SudoAdTrackerBlockerException.DataFormatException("mock")
        }

        shouldThrow<SudoAdTrackerBlockerException.DataFormatException> {
            adTrackerBlockerClient.checkUrl("a", "b", "c")
        }

        verify(mockBlockingProvider).checkIsUrlBlocked(anyString(), anyString(), anyString())
    }

    @Test
    fun `checkUrl() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockBlockingProvider.stub {
            onBlocking { checkIsUrlBlocked(anyString(), anyString(), anyString()) } doThrow CancellationException("mock")
        }

        shouldThrow<CancellationException> {
            adTrackerBlockerClient.checkUrl("a", "b", "c")
        }

        verify(mockBlockingProvider).checkIsUrlBlocked(anyString(), anyString(), anyString())
    }
}
