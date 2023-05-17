/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker

import android.content.Context
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import com.sudoplatform.sudoadtrackerblocker.TestData.S3_OBJECTS
import com.sudoplatform.sudologging.LogDriverInterface
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudoadtrackerblocker.TestData.USER_ID
import com.sudoplatform.sudoadtrackerblocker.TestData.USER_SUBJECT
import com.sudoplatform.sudoadtrackerblocker.blocking.BlockingProvider
import com.sudoplatform.sudoadtrackerblocker.rules.ActualPropertyResetter
import com.sudoplatform.sudoadtrackerblocker.rules.PropertyResetRule
import com.sudoplatform.sudoadtrackerblocker.rules.PropertyResetter
import com.sudoplatform.sudoadtrackerblocker.rules.TimberLogRule
import com.sudoplatform.sudoadtrackerblocker.s3.S3Client
import com.sudoplatform.sudoadtrackerblocker.storage.StorageProvider
import com.sudoplatform.sudouser.SudoUserClient
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.mockito.ArgumentMatchers.anyString

/**
 * Base class that sets up:
 * - [TimberLogRule]
 * - [PropertyResetRule]
 *
 * And provides convenient access to the [PropertyResetRule.before] via [PropertyResetter.before].
 */
internal abstract class BaseTests : PropertyResetter by ActualPropertyResetter() {
    @Rule @JvmField val timberLogRule = TimberLogRule()

    protected val mockContext by before {
        mock<Context>()
    }

    protected val mockLogDriver by before {
        mock<LogDriverInterface>().stub {
            on { logLevel } doReturn LogLevel.VERBOSE
        }
    }

    protected val mockLogger by before {
        Logger("mock", mockLogDriver)
    }

    protected val mockUserClient by before {
        mock<SudoUserClient>().stub {
            on { getUserName() } doReturn USER_ID
            on { getSubject() } doReturn USER_SUBJECT
        }
    }

    protected val mockS3Client by before {
        mock<S3Client>().stub {
            onBlocking { list(anyString(), any()) } doReturn S3_OBJECTS
            onBlocking { download(anyString()) } doReturn ByteArray(42)
        }
    }

    protected val mockStorageProvider by before {
        mock<StorageProvider>().stub {
            onBlocking { readFileETag(anyString()) } doReturn null
            onBlocking { readPreferences() } doReturn emptyMap()
        }
    }

    protected val mockBlockingProvider by before {
        mock<BlockingProvider>().stub {
            onBlocking { checkIsUrlBlocked(anyString(), anyString(), anyString()) } doReturn false
        }
    }

    protected val adTrackerBlockerClient by before {
        DefaultAdTrackerBlockerClient(
            context = mockContext,
            logger = mockLogger,
            sudoUserClient = mockUserClient,
            region = "region",
            bucket = "bucket",
            s3Client = mockS3Client,
            storageProvider = mockStorageProvider,
            blockingProvider = mockBlockingProvider
        ).apply {
            waitForClientInitToComplete(this)
        }
    }

    protected fun verifyMocksUsedInClientInit() = runBlocking<Unit> {
        if (adTrackerBlockerClient.status == SudoAdTrackerBlockerClient.FilterEngineStatus.READY) {
            verify(mockStorageProvider, atLeastOnce()).readPreferences()
            verify(mockBlockingProvider, atLeastOnce()).close()
        }
    }
}
