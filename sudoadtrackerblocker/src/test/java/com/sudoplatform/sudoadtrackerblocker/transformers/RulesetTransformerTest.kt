/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.transformers

import com.sudoplatform.sudoadtrackerblocker.s3.S3Client
import com.sudoplatform.sudoadtrackerblocker.transformers.RulesetTransformer.toRulesetType
import com.sudoplatform.sudoadtrackerblocker.types.Ruleset
import io.kotlintest.matchers.collections.shouldBeOneOf
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import org.junit.Test
import java.util.Date
import java.util.Locale

/**
 * Test the operation of [RulesetTransformer].
 */
class RulesetTransformerTest {

    @Test
    fun `transformer should convert all Ruleset types`() {
        val testCases = listOf(
            "AD",
            "PRIVACY",
            "UNKNOWN"
        )
        val types = Ruleset.Type.values().toList()
        testCases.forEach { testCase ->
            testCase.toRulesetType() shouldBeOneOf types
            testCase.toLowerCase(Locale.ROOT).toRulesetType() shouldBeOneOf types
            " $testCase ".toRulesetType() shouldBeOneOf types
        }
    }

    @Test
    fun `transformer should convert bad Ruleset types to unknown`() {
        val testCases = listOf(
            "xAD",
            "PRIVACYx",
            "UNKNOWN",
            "UNKNOWNs",
            ""
        )
        testCases.forEach { testCase ->
            testCase.toRulesetType() shouldBe Ruleset.Type.UNKNOWN
            testCase.toLowerCase(Locale.ROOT).toRulesetType() shouldBe Ruleset.Type.UNKNOWN
            " $testCase ".toRulesetType() shouldBe Ruleset.Type.UNKNOWN
        }
    }

    @Test
    fun `transformer should convert S3 data to Ruleset`() {
        val s3ObjectInfo = listOf(
            S3Client.S3ObjectInfo(
                key = "key",
                eTag = "42",
                lastModified = Date(1L),
                userMetadata = mapOf(
                    RulesetTransformer.METADATA_BLOB to """{
                        "${RulesetTransformer.METADATA_TYPE}": "AD"
                    }
                    """
                )
            ),
            S3Client.S3ObjectInfo(
                key = "key2",
                eTag = "43",
                lastModified = Date(1L),
                userMetadata = mapOf(
                    RulesetTransformer.METADATA_BLOB to """{
                        "${RulesetTransformer.METADATA_TYPE}": "unsupported"
                    }
                    """
                )
            ),
            S3Client.S3ObjectInfo(
                key = "key3",
                eTag = "44",
                lastModified = Date(1L),
                userMetadata = mapOf(
                    RulesetTransformer.METADATA_BLOB to "{"
                )
            )
        )
        val rulesetList = RulesetTransformer.toRulesetList(s3ObjectInfo)
        rulesetList shouldHaveSize 1
        with(rulesetList[0]) {
            id shouldBe "key"
            eTag shouldBe "42"
            updatedAt.time shouldBe 1L
            type shouldBe Ruleset.Type.AD_BLOCKING
        }
    }
}
