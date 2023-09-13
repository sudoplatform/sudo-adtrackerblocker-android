/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test the operation of [DefaultStorageProvider] under Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class StorageProviderTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val storageProvider = DefaultStorageProvider(context)
    private val fileName = "myFile"
    private val testData = "hello world"
    private val eTag = "eTag42"

    @Test
    fun checkReadWriteDelete() = runBlocking<Unit> {
        with(storageProvider) {
            read(fileName) shouldBe null
            delete(fileName) shouldBe false

            readFileETag(fileName) shouldBe null
            deleteFileETag(fileName) shouldBe false

            write(fileName, testData.toByteArray())
            val content = read(fileName)
            content shouldNotBe null
            String(content!!) shouldBe testData

            writeFileETag(fileName, eTag)
            readFileETag(fileName) shouldBe eTag
            deleteFileETag(fileName) shouldBe true

            delete(fileName) shouldBe true
            read(fileName) shouldBe null
            delete(fileName) shouldBe false

            readFileETag(fileName) shouldBe null
            deleteFileETag(fileName) shouldBe false
        }
    }

    @Test
    fun checkDeleteAll() = runBlocking<Unit> {
        with(storageProvider) {
            read(fileName) shouldBe null
            readFileETag(fileName) shouldBe null
            listFiles() shouldHaveSize 0

            write(fileName, testData.toByteArray())
            listFiles() shouldContainExactlyInAnyOrder listOf(fileName)

            writeFileETag(fileName, eTag)
            readFileETag(fileName) shouldBe eTag

            deleteFiles()
            deleteFileETags()

            readFileETag(fileName) shouldBe null
            read(fileName) shouldBe null
            listFiles() shouldHaveSize 0
        }
    }

    @Test
    fun checkPreferences() = runBlocking<Unit> {
        with(storageProvider) {
            readPreferences().keys shouldHaveSize 0

            val preferences1 = mapOf("1" to "a", "2" to "b")
            writePreferences(preferences1)

            val preferences2 = readPreferences()
            preferences2.keys shouldContainExactlyInAnyOrder setOf("1", "2")
            preferences2.values shouldContainExactlyInAnyOrder setOf("a", "b")
            preferences2 shouldBe preferences1

            val preferences3 = mapOf("a" to "1", "b" to "2")
            writePreferences(preferences3)

            val preferences4 = readPreferences()
            preferences4.keys shouldContainExactlyInAnyOrder setOf("a", "b")
            preferences4.values shouldContainExactlyInAnyOrder setOf("1", "2")
            preferences4 shouldBe preferences3

            deletePreferences()
            readPreferences().keys shouldHaveSize 0
        }
    }
}
