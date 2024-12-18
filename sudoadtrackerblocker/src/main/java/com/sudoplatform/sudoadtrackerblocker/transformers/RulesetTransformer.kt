/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.transformers

import androidx.annotation.VisibleForTesting
import com.sudoplatform.sudoadtrackerblocker.s3.S3Client
import com.sudoplatform.sudoadtrackerblocker.types.Ruleset
import org.json.JSONException
import org.json.JSONObject

/**
 * Transform from S3 types to publicly exposed [Ruleset]s and vice-versa.
 */
internal object RulesetTransformer {

    // S3 metadata items. This is what the user metadata in the S3 object looks like.
    // {sudoplatformblob={"categoryEnum":"AD","name.en":"EasyList"}}
    @VisibleForTesting
    const val METADATA_BLOB = "sudoplatformblob"

    @VisibleForTesting
    const val METADATA_TYPE = "categoryEnum"

    @VisibleForTesting
    const val METADATA_CATEGORY_AD = "AD"

    @VisibleForTesting
    const val METADATA_CATEGORY_PRIVACY = "PRIVACY"

    @VisibleForTesting
    const val METADATA_CATEGORY_SOCIAL = "SOCIAL"

    @VisibleForTesting
    const val APPLE_PATH = "/apple/"

    fun toRulesetList(s3ObjectInfoList: List<S3Client.S3ObjectInfo>): List<Ruleset> {
        return s3ObjectInfoList.filter {
            !it.key.contains(APPLE_PATH) &&
                extractRulesetTypeFromMetadata(it.userMetadata) != Ruleset.Type.UNKNOWN
        }.map {
            toRuleset(it)
        }
    }

    fun toRuleset(objectInfo: S3Client.S3ObjectInfo): Ruleset {
        return Ruleset(
            id = objectInfo.key,
            eTag = objectInfo.eTag,
            type = extractRulesetTypeFromMetadata(objectInfo.userMetadata),
            updatedAt = objectInfo.lastModified,
        )
    }

    private fun extractRulesetTypeFromMetadata(userMetadata: Map<String, String>): Ruleset.Type {
        return userMetadata[METADATA_BLOB]?.let { blob ->
            try {
                JSONObject(blob).getString(METADATA_TYPE).toRulesetType()
            } catch (e: JSONException) {
                null
            }
        } ?: Ruleset.Type.UNKNOWN
    }

    fun String?.toRulesetType(): Ruleset.Type {
        return when (this?.trim()) {
            METADATA_CATEGORY_AD -> Ruleset.Type.AD_BLOCKING
            METADATA_CATEGORY_PRIVACY -> Ruleset.Type.PRIVACY
            METADATA_CATEGORY_SOCIAL -> Ruleset.Type.SOCIAL
            else -> Ruleset.Type.UNKNOWN
        }
    }
}
