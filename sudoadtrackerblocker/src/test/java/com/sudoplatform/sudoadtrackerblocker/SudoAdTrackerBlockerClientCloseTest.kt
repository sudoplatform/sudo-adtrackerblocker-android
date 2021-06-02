/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudoadtrackerblocker

import org.mockito.kotlin.doThrow
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoAdTrackerBlockerClient.close] using mocks and spies.
 *
 * @since 2020-12-03
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoAdTrackerBlockerClientCloseTest : BaseTests() {

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
    }

    @Test
    fun `close() should call blocking provider`() = runBlocking<Unit> {

        adTrackerBlockerClient.close()

        verify(mockBlockingProvider, times(2)).close()
    }

    @Test
    fun `close() should suppress when blocking provider throws`() = runBlocking<Unit> {

        mockBlockingProvider.stub {
            on { close() } doThrow IOException("mock")
        }

        adTrackerBlockerClient.close()

        verify(mockBlockingProvider, times(2)).close()
    }

    @Test
    fun `close() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockBlockingProvider.stub {
            on { close() } doThrow CancellationException("mock")
        }

        shouldThrow<CancellationException> {
            adTrackerBlockerClient.close()
        }

        verify(mockBlockingProvider, times(2)).close()
    }
}
