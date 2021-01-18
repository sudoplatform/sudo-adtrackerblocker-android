/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.types

/**
 * An exception to the blocking rules that allow an entire host or a single
 * web page to be loaded despite the active blocking rulesets.
 *
 * @since 2020-12-08
 */
data class BlockingException(
    val source: String,
    val type: Type
) {
    enum class Type {
        /** The entire internet host is exempted from blocking */
        HOST,
        /** The page is exempted from blocking */
        PAGE
    }
}
