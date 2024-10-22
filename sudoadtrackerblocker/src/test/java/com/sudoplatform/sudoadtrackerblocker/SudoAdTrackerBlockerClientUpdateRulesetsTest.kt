/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker

import com.sudoplatform.sudoadtrackerblocker.DefaultAdTrackerBlockerClient.Companion.ADS_FILE
import com.sudoplatform.sudoadtrackerblocker.DefaultAdTrackerBlockerClient.Companion.PRIVACY_FILE
import com.sudoplatform.sudoadtrackerblocker.DefaultAdTrackerBlockerClient.Companion.S3_TOP_PATH
import com.sudoplatform.sudoadtrackerblocker.DefaultAdTrackerBlockerClient.Companion.SOCIAL_FILE
import com.sudoplatform.sudoadtrackerblocker.TestData.S3_PATH_ADS
import com.sudoplatform.sudoadtrackerblocker.TestData.S3_PATH_PRIVACY
import com.sudoplatform.sudoadtrackerblocker.TestData.S3_PATH_SOCIAL
import com.sudoplatform.sudoadtrackerblocker.s3.S3Exception
import com.sudoplatform.sudoadtrackerblocker.types.Ruleset
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoAdTrackerBlockerClient.updateRulesets] using mocks and spies.
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoAdTrackerBlockerClientUpdateRulesetsTest : BaseTests() {

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
        runBlocking {
            adTrackerBlockerClient.clearStorage()
        }
    }

    @Test
    fun `updateRulesets() should call S3 client`() = runBlocking<Unit> {
        adTrackerBlockerClient.updateRulesets()

        verify(mockS3Client).list(eq(S3_TOP_PATH), anyInt())
        verify(mockStorageProvider).readFileETag(ADS_FILE)
        verify(mockS3Client).download(eq(S3_PATH_ADS))
        verify(mockStorageProvider).write(eq(ADS_FILE), any())
        verify(mockStorageProvider).writeFileETag(eq(ADS_FILE), any())

        verify(mockStorageProvider).readFileETag(PRIVACY_FILE)
        verify(mockS3Client).download(eq(S3_PATH_PRIVACY))
        verify(mockStorageProvider).write(eq(PRIVACY_FILE), any())
        verify(mockStorageProvider).writeFileETag(eq(PRIVACY_FILE), any())

        verify(mockStorageProvider).readFileETag(SOCIAL_FILE)
        verify(mockS3Client).download(eq(S3_PATH_SOCIAL))
        verify(mockStorageProvider).write(eq(SOCIAL_FILE), any())
        verify(mockStorageProvider).writeFileETag(eq(SOCIAL_FILE), any())
    }

    @Test
    fun `updateRulesets() should return none when unsupported ruleset type is passed`() = runBlocking<Unit> {
        adTrackerBlockerClient.updateRulesets(Ruleset.Type.UNKNOWN)
    }

    @Test
    fun `updateRulesets() should return null when s3 client download fails`() = runBlocking<Unit> {
        mockS3Client.stub {
            onBlocking { download(anyString()) } doThrow S3Exception.DownloadException("mock")
        }

        adTrackerBlockerClient.updateRulesets()

        verify(mockStorageProvider).readFileETag(anyString())
        verify(mockS3Client).list(anyString(), anyInt())
        verify(mockS3Client).download(anyString())
    }

    @Test
    fun `updateRulesets() should throw when s3 client throws`() = runBlocking<Unit> {
        mockS3Client.stub {
            onBlocking { download(anyString()) } doThrow IllegalStateException("mock")
        }

        shouldThrow<SudoAdTrackerBlockerException.UnknownException> {
            adTrackerBlockerClient.updateRulesets()
        }

        verify(mockStorageProvider).readFileETag(anyString())
        verify(mockS3Client).list(anyString(), anyInt())
        verify(mockS3Client).download(anyString())
    }

    @Test
    fun `updateRulesets() should not block coroutine cancellation exception`() = runBlocking<Unit> {
        mockS3Client.stub {
            onBlocking { download(anyString()) } doThrow CancellationException("Mock")
        }

        shouldThrow<CancellationException> {
            adTrackerBlockerClient.updateRulesets()
        }

        verify(mockStorageProvider).readFileETag(anyString())
        verify(mockS3Client).list(anyString(), anyInt())
        verify(mockS3Client).download(anyString())
    }
}
