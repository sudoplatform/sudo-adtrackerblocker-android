/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.types

import java.util.EnumSet

/**
 * [Ruleset] extensions.
 */

/**
 * Returns an [Array] of all the [Ruleset.Type]s except for the catchall [Ruleset.Type.UNKNOWN].
 */
fun allRulesets(): Array<Ruleset.Type> {
    return EnumSet.complementOf(EnumSet.of(Ruleset.Type.UNKNOWN)).toTypedArray()
}

/**
 * Returns an empty [Array] of [Ruleset.Type]s.
 */
fun noRulesets() = emptyArray<Ruleset.Type>()
