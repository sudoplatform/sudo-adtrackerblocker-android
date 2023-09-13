/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker

import com.sudoplatform.sudoadtrackerblocker.DefaultAdTrackerBlockerClient.Companion.ADS_FILE
import com.sudoplatform.sudoadtrackerblocker.DefaultAdTrackerBlockerClient.Companion.ADS_SUBPATH
import com.sudoplatform.sudoadtrackerblocker.DefaultAdTrackerBlockerClient.Companion.PRIVACY_FILE
import com.sudoplatform.sudoadtrackerblocker.DefaultAdTrackerBlockerClient.Companion.PRIVACY_SUBPATH
import com.sudoplatform.sudoadtrackerblocker.DefaultAdTrackerBlockerClient.Companion.S3_TOP_PATH
import com.sudoplatform.sudoadtrackerblocker.DefaultAdTrackerBlockerClient.Companion.SOCIAL_FILE
import com.sudoplatform.sudoadtrackerblocker.DefaultAdTrackerBlockerClient.Companion.SOCIAL_SUBPATH
import com.sudoplatform.sudoadtrackerblocker.s3.S3Client
import com.sudoplatform.sudoadtrackerblocker.transformers.RulesetTransformer
import java.util.Date
import java.util.UUID

/**
 * Data common to many tests.
 */
internal object TestData {

    const val USER_ID = "slartibartfast"
    val USER_SUBJECT = UUID.randomUUID().toString()

    const val S3_PATH_ADS = "$S3_TOP_PATH/$ADS_SUBPATH/$ADS_FILE"
    const val S3_PATH_PRIVACY = "$S3_TOP_PATH/$PRIVACY_SUBPATH/$PRIVACY_FILE"
    const val S3_PATH_SOCIAL = "$S3_TOP_PATH/$SOCIAL_SUBPATH/$SOCIAL_FILE"

    val S3_AD_OBJECT_USER_METADATA = mapOf(
        RulesetTransformer.METADATA_BLOB to """{
            "${RulesetTransformer.METADATA_TYPE}": "${RulesetTransformer.METADATA_CATEGORY_AD}"
        }
        """
    )
    private val S3_PRIVACY_OBJECT_USER_METADATA = mapOf(
        RulesetTransformer.METADATA_BLOB to """{
            "${RulesetTransformer.METADATA_TYPE}": "${RulesetTransformer.METADATA_CATEGORY_PRIVACY}"
        }
        """
    )
    private val S3_SOCIAL_OBJECT_USER_METADATA = mapOf(
        RulesetTransformer.METADATA_BLOB to """{
            "${RulesetTransformer.METADATA_TYPE}": "${RulesetTransformer.METADATA_CATEGORY_SOCIAL}"
        }
        """
    )

    val S3_OBJECTS = listOf(
        S3Client.S3ObjectInfo(
            key = "ad",
            eTag = "etag1",
            lastModified = Date(1L),
            userMetadata = S3_AD_OBJECT_USER_METADATA
        ),
        S3Client.S3ObjectInfo(
            key = "privacy",
            eTag = "etag2",
            lastModified = Date(1L),
            userMetadata = S3_PRIVACY_OBJECT_USER_METADATA
        ),
        S3Client.S3ObjectInfo(
            key = "social",
            eTag = "etag3",
            lastModified = Date(1L),
            userMetadata = S3_SOCIAL_OBJECT_USER_METADATA
        )
    )

    val PRIVACY_VIOLATORS = mapOf(
        "youtube.com/ptracking?" to "youtube.com"
    )
    val ADVERTISERS = mapOf(
        "ad.doubleclick.net" to "ad.doubleclick.net"
    )
    val SHOULD_NOT_BE_BLOCKED = mapOf(
        "anonyome.com/about.js" to "anonyome.com",
        "mysudo.com/support/foo.js" to "mysudo.com",
        "brisbanetimes.com.au" to "brisbanetimes.com.au"
    )
}
