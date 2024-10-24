/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker

import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoAdTrackerBlockerClient.clearStorage] using mocks and spies.
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoAdTrackerBlockerClientClearStorageTest : BaseTests() {
    @After
    fun fini() {
        verifyMocksUsedInClientInit()
        verifyNoMoreInteractions(
            mockContext,
            mockUserClient,
            mockS3Client,
            mockStorageProvider,
            mockBlockingProvider,
        )
    }

    @Test
    fun `reset() should call storage provider`() = runBlocking<Unit> {
        adTrackerBlockerClient.clearStorage()

        verify(mockStorageProvider).deleteFiles()
        verify(mockStorageProvider).deleteFileETags()
        verify(mockStorageProvider).deletePreferences()
    }

    @Test
    fun `reset() should throw when storage provider throws`() = runBlocking<Unit> {
        mockStorageProvider.stub {
            onBlocking { deleteFiles() } doThrow IOException("mock")
        }

        shouldThrow<SudoAdTrackerBlockerException.FailedException> {
            adTrackerBlockerClient.clearStorage()
        }

        verify(mockStorageProvider).deleteFiles()
    }

    @Test
    fun `reset() should throw when storage provider throws from eTags`() = runBlocking<Unit> {
        mockStorageProvider.stub {
            onBlocking { deleteFileETags() } doThrow IOException("mock")
        }

        shouldThrow<SudoAdTrackerBlockerException.FailedException> {
            adTrackerBlockerClient.clearStorage()
        }

        verify(mockStorageProvider).deleteFiles()
        verify(mockStorageProvider).deleteFileETags()
    }

    @Test
    fun `reset() should throw when storage provider throws from preferences`() = runBlocking<Unit> {
        mockStorageProvider.stub {
            onBlocking { deletePreferences() } doThrow IOException("mock")
        }

        shouldThrow<SudoAdTrackerBlockerException.FailedException> {
            adTrackerBlockerClient.clearStorage()
        }

        verify(mockStorageProvider).deleteFiles()
        verify(mockStorageProvider).deleteFileETags()
        verify(mockStorageProvider).deletePreferences()
    }

    @Test
    fun `reset() should not block coroutine cancellation exception`() = runBlocking<Unit> {
        mockStorageProvider.stub {
            onBlocking { deleteFiles() } doThrow CancellationException("mock")
        }

        shouldThrow<CancellationException> {
            adTrackerBlockerClient.clearStorage()
        }

        verify(mockStorageProvider).deleteFiles()
    }
}
