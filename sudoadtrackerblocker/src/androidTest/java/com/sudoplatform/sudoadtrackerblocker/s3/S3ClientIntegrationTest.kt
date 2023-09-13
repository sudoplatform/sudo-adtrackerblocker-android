/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.s3

import com.sudoplatform.sudoadtrackerblocker.BaseIntegrationTest
import com.sudoplatform.sudoadtrackerblocker.DefaultAdTrackerBlockerClient
import com.sudoplatform.sudoadtrackerblocker.TestData.S3_PATH_ADS
import com.sudoplatform.sudoadtrackerblocker.checkEasyList
import io.kotlintest.matchers.collections.shouldHaveAtLeastSize
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber

/**
 * Test the operation of the [DefaultS3Client].
 */
class S3ClientIntegrationTest : BaseIntegrationTest() {

    private lateinit var s3Client: S3Client

    @Before
    fun init() {
        Timber.plant(Timber.DebugTree())

        if (clientConfigFilesPresent()) {
            val config = readS3Configuration(context, logger)
            s3Client = DefaultS3Client(
                context = context,
                sudoUserClient = userClient,
                logger = logger,
                bucket = config.bucket,
                region = config.region
            )
        }
    }

    @After
    fun fini() {
        Timber.uprootAll()
    }

    @Test
    fun listShouldReturnObjectInfo() = runBlocking<Unit> {
        assumeTrue(clientConfigFilesPresent())

        signInAndRegisterUser()

        val objects = s3Client.list(DefaultAdTrackerBlockerClient.S3_TOP_PATH)
        objects shouldHaveAtLeastSize 1
    }

    @Test
    fun downloadShouldGetObject() = runBlocking<Unit> {
        assumeTrue(clientConfigFilesPresent())

        signInAndRegisterUser()

        checkEasyList(s3Client.download(S3_PATH_ADS))
    }
}
