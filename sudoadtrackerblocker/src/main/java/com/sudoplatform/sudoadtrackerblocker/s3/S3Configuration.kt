/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.s3

import android.content.Context
import com.sudoplatform.sudoadtrackerblocker.SudoAdTrackerBlockerException
import com.sudoplatform.sudoconfigmanager.DefaultSudoConfigManager
import com.sudoplatform.sudoconfigmanager.SudoConfigManager
import com.sudoplatform.sudologging.Logger
import org.json.JSONException

private const val CONFIG_AD_TRACKER_BLOCKER_SERVICE = "adTrackerBlockerService"
private const val CONFIG_REGION = "region"
private const val CONFIG_STATIC_DATA_BUCKET = "bucket"

internal data class S3Configuration(
    val region: String,
    val bucket: String,
)

/**
 * Read the S3 configuration elements from the Sudo Configuration Manager
 */
@Throws(SudoAdTrackerBlockerException.ConfigurationException::class)
internal fun readS3Configuration(
    context: Context,
    logger: Logger,
    configManager: SudoConfigManager = DefaultSudoConfigManager(context, logger),
): S3Configuration {
    val preamble = "sudoplatformconfig.json does not contain"
    val postamble = "the $CONFIG_AD_TRACKER_BLOCKER_SERVICE stanza"

    val identityConfig = try {
        configManager.getConfigSet(CONFIG_AD_TRACKER_BLOCKER_SERVICE)
    } catch (e: JSONException) {
        throw SudoAdTrackerBlockerException.ConfigurationException("$preamble $postamble", e)
    }
    identityConfig ?: throw SudoAdTrackerBlockerException.ConfigurationException("$preamble $postamble")

    val region = try {
        identityConfig.getString(CONFIG_REGION)
    } catch (e: JSONException) {
        throw SudoAdTrackerBlockerException.ConfigurationException("$preamble $CONFIG_REGION in $postamble", e)
    }

    val bucket = try {
        identityConfig.getString(CONFIG_STATIC_DATA_BUCKET)
    } catch (e: JSONException) {
        throw SudoAdTrackerBlockerException.ConfigurationException("$preamble $CONFIG_STATIC_DATA_BUCKET in $postamble", e)
    }

    return S3Configuration(region, bucket)
}
