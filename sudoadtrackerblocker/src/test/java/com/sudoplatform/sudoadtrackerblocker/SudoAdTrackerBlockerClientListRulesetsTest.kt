/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudoadtrackerblocker

import com.amazonaws.services.cognitoidentity.model.NotAuthorizedException
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import com.sudoplatform.sudoadtrackerblocker.s3.S3Exception
import com.sudoplatform.sudouser.exceptions.AuthenticationException
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.should
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoAdTrackerBlockerClient.listRulesets] using mocks and spies.
 *
 * @since 2020-11-25
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoAdTrackerBlockerClientListRulesetsTest : BaseTests() {

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
    fun `listRulesets() should call S3 client`() = runBlocking<Unit> {

        adTrackerBlockerClient.listRulesets() shouldHaveSize 3

        verify(mockS3Client, atLeastOnce()).list(eq(DefaultAdTrackerBlockerClient.S3_TOP_PATH), any())
    }

    @Test
    fun `listRulesets() should return none when S3 client does`() = runBlocking<Unit> {

        mockS3Client.stub {
            onBlocking { list(anyString(), any()) } doReturn emptyList()
        }

        adTrackerBlockerClient.listRulesets() shouldHaveSize 0

        verify(mockS3Client, atLeastOnce()).list(eq(DefaultAdTrackerBlockerClient.S3_TOP_PATH), any())
    }

    @Test
    fun `listRulesets() should throw when s3 client throws`() = runBlocking<Unit> {

        mockS3Client.stub {
            onBlocking { list(anyString(), any()) } doThrow S3Exception.DownloadException("mock")
        }

        shouldThrow<SudoAdTrackerBlockerException.FailedException> {
            adTrackerBlockerClient.listRulesets()
        }

        verify(mockS3Client, atLeastOnce()).list(eq(DefaultAdTrackerBlockerClient.S3_TOP_PATH), any())
    }

    @Test
    fun `listRulesets() should transform s3 client not authorized exception`() = runBlocking<Unit> {

        // SudoUser NotAuthorizedException should be transformed.
        reset(mockS3Client)
        mockS3Client.stub {
            onBlocking { list(anyString(), any()) } doThrow AuthenticationException.NotAuthorizedException("mock")
        }

        with(
            shouldThrow<SudoAdTrackerBlockerException.UnauthorizedUserException> {
                adTrackerBlockerClient.listRulesets()
            }
        ) {
            cause should beInstanceOf<AuthenticationException.NotAuthorizedException>()
        }

        verify(mockS3Client, atLeastOnce()).list(eq(DefaultAdTrackerBlockerClient.S3_TOP_PATH), any())

        // AWS NotAuthorizedException should be transformed.
        reset(mockS3Client)
        mockS3Client.stub {
            onBlocking { list(anyString(), any()) } doThrow NotAuthorizedException("mock")
        }

        with(
            shouldThrow<SudoAdTrackerBlockerException.UnauthorizedUserException> {
                adTrackerBlockerClient.listRulesets()
            }
        ) {
            cause should beInstanceOf<NotAuthorizedException>()
        }

        verify(mockS3Client, atLeastOnce()).list(eq(DefaultAdTrackerBlockerClient.S3_TOP_PATH), any())
    }

    @Test
    fun `listRulesets() should throw when s3 client gets bad metadata`() = runBlocking<Unit> {

        mockS3Client.stub {
            onBlocking { list(anyString(), any()) } doThrow S3Exception.MetadataException("mock")
        }

        shouldThrow<SudoAdTrackerBlockerException.DataFormatException> {
            adTrackerBlockerClient.listRulesets()
        }

        verify(mockS3Client, atLeastOnce()).list(eq(DefaultAdTrackerBlockerClient.S3_TOP_PATH), any())
    }

    @Test
    fun `listRulesets() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockS3Client.stub {
            onBlocking { list(anyString(), any()) } doThrow CancellationException("Mock")
        }

        shouldThrow<CancellationException> {
            adTrackerBlockerClient.listRulesets()
        }

        verify(mockS3Client, atLeastOnce()).list(eq(DefaultAdTrackerBlockerClient.S3_TOP_PATH), any())
    }
}
