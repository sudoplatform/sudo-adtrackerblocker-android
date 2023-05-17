/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.blocking

import com.sudoplatform.sudoadtrackerblocker.SudoAdTrackerBlockerException

/**
 * Provide ad and tracker detection and blocking.
 */
interface BlockingProvider : AutoCloseable {

    /**
     * Set the rules the blocking service should use to determine if a URL should be blocked.
     *
     * @param blockingRules The sets of blocking rules to use.
     * @param exceptionRules The exceptions to the blocking rules.
     */
    suspend fun setRules(blockingRules: List<ByteArray>, exceptionRules: ByteArray? = null)

    /**
     * Checks a URL to determine if it is blocked according to current configuration.
     *
     * @param url The URL of the resource that should be checked against the currently active rulesets and the exceptions list
     * @param sourceUrl The URL of the page that requested the [url] be loaded
     * @param resourceType The MIME type of the resource indicated by the [url], null if it is not known
     * @return true if the URL is blocked, false if it can be loaded
     */
    @Throws(SudoAdTrackerBlockerException::class)
    suspend fun checkIsUrlBlocked(url: String, sourceUrl: String?, resourceType: String?): Boolean
}
