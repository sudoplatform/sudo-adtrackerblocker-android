/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.transformers

import android.net.Uri
import com.sudoplatform.sudoadtrackerblocker.types.BlockingException

/**
 * Transform from a set of [BlockingException]s to a set of EasyList rules that can be used
 * to match URLs against the exception list of stuff that should not be blocked.
 * This class will not create exceptions as described below rather it will define
 * a set of rules to MATCH the items in the exception list. This will allow an
 * adblock engine to be used to match an item in the exception list and then the
 * result will be inverted so that instead of indicating the URL should be blocked
 * it will be used to indicate the item matches the exception list.
 *
 * https://help.eyeo.com/en/adblockplus/how-to-write-filters
 * https://adblockplus.org/en/filter-cheatsheet
 *
 * @since 2020-12-03
 */
internal object ExceptionsTransformer {

    fun toExceptionRules(exceptions: Set<BlockingException>): ByteArray? {
        if (exceptions.isEmpty()) {
            return null
        }
        val rules = StringBuilder()
        for (exception in exceptions) {
            var uri = Uri.parse(exception.source)
            if (uri.scheme == null) {
                // There is no http/https on the front
                uri = Uri.parse("scheme://${exception.source}")
            }
            if (exception.type == BlockingException.Type.HOST) {
                // Match by host
                rules.appendLine("|http://${uri.host}^")
                rules.appendLine("|https://${uri.host}^")
            } else {
                // Match by host and path
                rules.appendLine("|http://${uri.host}${uri.path}^")
                rules.appendLine("|https://${uri.host}${uri.path}^")
            }
        }
        return rules.toString().toByteArray(Charsets.UTF_8)
    }
}
