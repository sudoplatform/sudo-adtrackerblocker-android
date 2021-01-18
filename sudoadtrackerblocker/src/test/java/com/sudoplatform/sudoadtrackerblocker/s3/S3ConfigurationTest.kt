/*
 * Copyright © 2020 - Anonyome Labs, Inc. - All rights reserved
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.s3

import com.sudoplatform.sudoadtrackerblocker.BaseTests
import com.sudoplatform.sudoadtrackerblocker.SudoAdTrackerBlockerException
import com.sudoplatform.sudoadtrackerblocker.logging.LogConstants
import com.sudoplatform.sudoconfigmanager.SudoConfigManager
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import io.kotlintest.shouldThrow
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test the handling of the JSON config items.
 *
 * @since 2020-11-18
 */
@RunWith(RobolectricTestRunner::class)
internal class S3ConfigurationTest : BaseTests() {

    private fun configManager(configJson: String): SudoConfigManager {
        return object : SudoConfigManager {
            override fun getConfigSet(namespace: String): JSONObject? {
                if (namespace == "identityService") {
                    return JSONObject(configJson)
                }
                return null
            }
        }
    }

    @Test
    fun shouldThrowIfConfigMissing() {

        val logger = com.sudoplatform.sudologging.Logger(LogConstants.SUDOLOG_TAG, AndroidUtilsLogDriver(LogLevel.INFO))

        val noConfigJson = ""
        shouldThrow<SudoAdTrackerBlockerException.ConfigurationException> {
            readS3Configuration(mockContext, logger, configManager(noConfigJson))
        }

        val emptyConfigJson = "{}"
        shouldThrow<SudoAdTrackerBlockerException.ConfigurationException> {
            readS3Configuration(mockContext, logger, configManager(emptyConfigJson))
        }

        val missingRegionJson = """
            {
                "staticDataBucket": "ids-userdata-eml-dev-transientuserdatabucket0d043-5tkr1hts9sja"
            }
        """.trimIndent()

        shouldThrow<SudoAdTrackerBlockerException.ConfigurationException> {
            readS3Configuration(mockContext, logger, configManager(missingRegionJson))
        }

        val missingBucketJson = """
            {
                "region": "us-east-1"
                "transientBucket": "ids-userdata-eml-dev-transientuserdatabucket0d043-5tkr1hts9sja"
            }
        """.trimIndent()

        shouldThrow<SudoAdTrackerBlockerException.ConfigurationException> {
            readS3Configuration(mockContext, logger, configManager(missingBucketJson))
        }

        val completeConfigJson = """
            {
                "region": "us-east-1",
                "staticDataBucket": "foo"
            }
        """.trimIndent()

        readS3Configuration(mockContext, logger, configManager(completeConfigJson))
    }
}
