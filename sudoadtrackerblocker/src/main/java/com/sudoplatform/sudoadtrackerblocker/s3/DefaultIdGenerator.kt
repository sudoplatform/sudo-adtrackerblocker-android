/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.s3

import com.sudoplatform.sudouser.IdGenerator
import java.util.Locale
import java.util.UUID

/**
 * Generates unique identifiers for S3 objects
 *
 * @since 2020-11-17
 */
internal class DefaultIdGenerator : IdGenerator {
    override fun generateId(): String {
        return UUID.randomUUID().toString().toUpperCase(Locale.ROOT)
    }
}
