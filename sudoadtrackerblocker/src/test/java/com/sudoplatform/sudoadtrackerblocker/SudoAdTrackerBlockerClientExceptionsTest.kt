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
import com.sudoplatform.sudoadtrackerblocker.blocking.BlockingExceptions
import com.sudoplatform.sudoadtrackerblocker.types.toHostException
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoAdTrackerBlockerClient.addExceptions], [SudoAdTrackerBlockerClient.getExceptions],
 * [SudoAdTrackerBlockerClient.removeExceptions] and [SudoAdTrackerBlockerClient.removeAllExceptions] using mocks and spies.
 *
 * @since 2020-12-07
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoAdTrackerBlockerClientExceptionsTest : BaseTests() {

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

    private fun verifyReadAndWriteOfExceptionsFile() {
        verify(mockStorageProvider).read(BlockingExceptions.EXCEPTIONS_FILE)
        verify(mockStorageProvider).write(anyString(), any())
    }

    private val hostException = toHostException(TestData.ADVERTISERS.keys.first())

    //
    // addExceptions
    //

    @Test
    fun `addExceptions() should call storage and blocking provider`() = runBlocking<Unit> {

        adTrackerBlockerClient.addExceptions(hostException)

        verifyReadAndWriteOfExceptionsFile()
        verify(mockBlockingProvider, times(2)).close()
    }

    @Test
    fun `addExceptions() should throw when storage provider throws`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { write(anyString(), any()) } doThrow IOException("mock")
        }

        shouldThrow<SudoAdTrackerBlockerException.FailedException> {
            adTrackerBlockerClient.addExceptions(hostException)
        }

        verifyReadAndWriteOfExceptionsFile()
    }

    @Test
    fun `addExceptions() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { read(anyString()) } doThrow CancellationException("Mock")
        }

        shouldThrow<CancellationException> {
            adTrackerBlockerClient.addExceptions(hostException)
        }

        verify(mockStorageProvider).read(BlockingExceptions.EXCEPTIONS_FILE)
    }

    //
    // removeExceptions
    //

    @Test
    fun `removeExceptions() should call storage and blocking provider`() = runBlocking<Unit> {

        adTrackerBlockerClient.removeExceptions(hostException)

        verifyReadAndWriteOfExceptionsFile()
        verify(mockBlockingProvider, times(2)).close()
    }

    @Test
    fun `removeExceptions() should throw when storage provider throws`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { write(anyString(), any()) } doThrow IOException("mock")
        }

        shouldThrow<SudoAdTrackerBlockerException.FailedException> {
            adTrackerBlockerClient.removeExceptions(hostException)
        }

        verifyReadAndWriteOfExceptionsFile()
    }

    @Test
    fun `removeExceptions() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { read(anyString()) } doThrow CancellationException("Mock")
        }

        shouldThrow<CancellationException> {
            adTrackerBlockerClient.removeExceptions(hostException)
        }

        verify(mockStorageProvider).read(BlockingExceptions.EXCEPTIONS_FILE)
    }

    //
    // removeAllExceptions
    //

    @Test
    fun `removeAllExceptions() should call storage and blocking provider`() = runBlocking<Unit> {

        adTrackerBlockerClient.removeAllExceptions()

        verify(mockStorageProvider).delete(BlockingExceptions.EXCEPTIONS_FILE)
        verify(mockBlockingProvider, times(2)).close()
    }

    @Test
    fun `removeAllExceptions() should throw when storage provider throws`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { delete(anyString()) } doThrow IOException("mock")
        }

        shouldThrow<SudoAdTrackerBlockerException.FailedException> {
            adTrackerBlockerClient.removeAllExceptions()
        }

        verify(mockStorageProvider).delete(BlockingExceptions.EXCEPTIONS_FILE)
    }

    @Test
    fun `removeAllExceptions() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { delete(anyString()) } doThrow CancellationException("Mock")
        }

        shouldThrow<CancellationException> {
            adTrackerBlockerClient.removeAllExceptions()
        }

        verify(mockStorageProvider).delete(BlockingExceptions.EXCEPTIONS_FILE)
    }

    //
    // getExceptions
    //

    @Test
    fun `getExceptions() should call storage and blocking provider`() = runBlocking<Unit> {

        adTrackerBlockerClient.getExceptions()

        verify(mockStorageProvider).read(BlockingExceptions.EXCEPTIONS_FILE)
    }

    @Test
    fun `getExceptions() should throw when storage provider throws`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { read(anyString()) } doThrow IOException("mock")
        }

        shouldThrow<SudoAdTrackerBlockerException.FailedException> {
            adTrackerBlockerClient.getExceptions()
        }

        verify(mockStorageProvider).read(BlockingExceptions.EXCEPTIONS_FILE)
    }

    @Test
    fun `getExceptions() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockStorageProvider.stub {
            onBlocking { read(anyString()) } doThrow CancellationException("Mock")
        }

        shouldThrow<CancellationException> {
            adTrackerBlockerClient.getExceptions()
        }

        verify(mockStorageProvider).read(BlockingExceptions.EXCEPTIONS_FILE)
    }
}
