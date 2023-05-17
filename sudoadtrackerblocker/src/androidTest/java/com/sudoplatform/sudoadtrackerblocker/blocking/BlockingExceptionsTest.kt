/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.blocking

import com.sudoplatform.sudoadtrackerblocker.BaseIntegrationTest
import com.sudoplatform.sudoadtrackerblocker.storage.DefaultStorageProvider
import com.sudoplatform.sudoadtrackerblocker.types.BlockingException
import com.sudoplatform.sudoadtrackerblocker.types.toHostException
import com.sudoplatform.sudoadtrackerblocker.types.toPageException
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldHaveSize
import org.junit.After
import org.junit.Before
import org.junit.Test
import timber.log.Timber

/**
 * Test the operation of [BlockingExceptions] on a Android device or emulator.
 */
class BlockingExceptionsTest : BaseIntegrationTest() {

    private val storageProvider = DefaultStorageProvider(context)
    private val blockingExceptions = BlockingExceptions(storageProvider)

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() {
        storageProvider.deleteFiles()
        Timber.uprootAll()
    }

    @Test
    fun shouldBeAbleToReadAndWriteSmallSet() {

        blockingExceptions.readExceptions() shouldHaveSize 0

        blockingExceptions.writeExceptions(setOf(toPageException("example.com")))
        blockingExceptions.readExceptions() shouldContainExactly setOf(toPageException("example.com"))

        blockingExceptions.deleteExceptions()
        blockingExceptions.readExceptions() shouldHaveSize 0
    }

    @Test
    fun shouldBeAbleToReadAndWriteBigSet() {

        blockingExceptions.readExceptions() shouldHaveSize 0

        val exceptions = mutableSetOf<BlockingException>()
        for (i in 1..10_000) {
            exceptions.add(toHostException("https://$i.domain.com"))
        }
        blockingExceptions.writeExceptions(exceptions)
        blockingExceptions.readExceptions() shouldContainExactly exceptions

        blockingExceptions.deleteExceptions()
        blockingExceptions.readExceptions() shouldHaveSize 0
    }
}
