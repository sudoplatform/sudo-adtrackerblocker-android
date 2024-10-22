/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.blocking

import com.sudoplatform.sudoadtrackerblocker.FilterEngine
import com.sudoplatform.sudologging.Logger
import java.util.concurrent.CancellationException

/**
 * Default implementation of a provider of ad and tracker detection and blocking.
 */
internal class DefaultBlockingProvider(private val logger: Logger) : BlockingProvider {

    private var filterEngine: FilterEngine? = null
    private var exceptionEngine: FilterEngine? = null

    override suspend fun setRules(blockingRules: List<String>, exceptionRules: String?) {
        try {
            close()
            filterEngine = FilterEngine(blockingRules)
            exceptionEngine = exceptionRules?.let {
                FilterEngine(listOf(it))
            }

            logger.info("Blocking provider initialization completed successfully. ${blockingRules.size} blocker(s) are active.")
        } catch (e: CancellationException) {
            // Never suppress this exception it's used by coroutines to cancel outstanding work
            throw e
        } catch (e: Throwable) {
            logger.error("Blocking engine initialization failed $e")
            throw e
        }
    }

    override fun close() {
        synchronized(this) {
            exceptionEngine?.close()
            exceptionEngine = null
            filterEngine?.close()
            filterEngine = null
        }
    }

    override suspend fun checkIsUrlBlocked(url: String, sourceUrl: String?, resourceType: String?): Boolean {
        synchronized(this) {
            if (!sourceUrl.isNullOrBlank() &&
                isInExceptionList(sourceUrl, resourceType)
            ) {
                return false
            }

            // filter engine wants a full url with a scheme
            val checkURL = "https://" + url.removePrefix("http://").removePrefix("https://")
            return filterEngine?.checkNetworkUrlsMatched(
                checkURL,
                sourceUrl ?: "",
                resourceType ?: "script",
            ) ?: false
        }

        return false
    }

    private fun isInExceptionList(
        currentUrl: String,
        resourceType: String?,
    ): Boolean {
        val exceptionEngine = this.exceptionEngine
            ?: return false

        val checkURL = "https://" + currentUrl.removePrefix("http://").removePrefix("https://")
        return exceptionEngine.checkNetworkUrlsMatched(checkURL, "", resourceType ?: "script")
    }
}
