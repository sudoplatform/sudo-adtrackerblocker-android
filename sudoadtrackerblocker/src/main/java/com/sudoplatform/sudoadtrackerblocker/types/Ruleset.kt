/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.types

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * A set of rules that provide:
 * - rules to detect requests for URLs by a browser that are for advertising or user tracking
 * - rules to detect URLs involving social media
 */
@Parcelize
data class Ruleset(
    /** The unique identifier of the ruleset */
    val id: String,
    /** The type of the ruleset */
    val type: Type,
    /** The eTag that is used to detect out of date rulesets */
    val eTag: String,
    /** When this ruleset was last updated */
    val updatedAt: Date
) : Parcelable {
    enum class Type {
        AD_BLOCKING,
        PRIVACY,
        SOCIAL,
        UNKNOWN
    }
}
