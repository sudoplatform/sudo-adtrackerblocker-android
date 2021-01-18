/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudoadtrackerblocker.blocking

import android.net.Uri
import com.sudoplatform.sudoadtrackerblocker.blocking.adblock.AdBlockEngine
import com.sudoplatform.sudologging.Logger
import java.util.concurrent.CancellationException

/**
 * Default implementation of a provider of ad and tracker detection and blocking.
 *
 * @since 2020-12-02
 */
internal class DefaultBlockingProvider(private val logger: Logger) : BlockingProvider {

    private val adBlockEngines = mutableListOf<AdBlockEngine>()
    private var exceptionEngine: AdBlockEngine? = null
    private var adBlockEngineError: Throwable? = null

    override suspend fun setRules(blockingRules: List<ByteArray>, exceptionRules: ByteArray?) {
        try {
            close()

            val newAdBlockEngines = blockingRules.map { rules ->
                AdBlockEngine().apply {
                    loadRules(String(rules, Charsets.UTF_8))
                }
            }

            val newExceptionEngine = exceptionRules?.let { rules ->
                AdBlockEngine().apply {
                    loadRules(String(rules, Charsets.UTF_8))
                }
            }

            synchronized(adBlockEngines) {
                adBlockEngines.clear()
                adBlockEngines.addAll(newAdBlockEngines)
                exceptionEngine = newExceptionEngine
            }

            logger.info("Blocking provider initialization completed successfully. ${adBlockEngines.size} blocker(s) are active.")
        } catch (e: CancellationException) {
            // Never suppress this exception it's used by coroutines to cancel outstanding work
            throw e
        } catch (e: Throwable) {
            logger.error("Blocking engine initialization failed $e")
            throw e
        }
    }

    override fun close() {
        adBlockEngineError = null
        synchronized(adBlockEngines) {
            adBlockEngines.forEach { engine ->
                engine.close()
            }
            exceptionEngine?.close()
            exceptionEngine = null
            adBlockEngines.clear()
        }
    }

    override suspend fun checkIsUrlBlocked(url: String, sourceUrl: String?, resourceType: String?): Boolean {

        val requestHost = Uri.parse(url).host ?: ""
        val sourceHost = sourceUrl?.let { Uri.parse(it).host } ?: ""

        synchronized(adBlockEngines) {

            if (!sourceUrl.isNullOrBlank() &&
                isInExceptionList(sourceUrl, resourceType, requestHost, sourceHost)) {
                return false
            }

            adBlockEngines.forEach { blockingEngine ->
                val isAllowed = blockingEngine.shouldLoad(
                    url,
                    sourceUrl ?: "",
                    resourceType,
                    requestHost,
                    sourceHost
                )
                if (!isAllowed) {
                    return true
                }
            }
        }

        return false
    }

    private fun isInExceptionList(
        currentUrl: String,
        resourceType: String?,
        requestHost: String,
        sourceHost: String
    ): Boolean {
        val exceptionEngine = this.exceptionEngine
            ?: return false

        // This is negated because we are using rules loaded into the exception engine to
        // match the exception list. Therefore if the exception engine says it's OK to load
        // a URL that means the URL is not in the exception list.
        return !exceptionEngine.shouldLoad(
                currentUrl,
                "",
                resourceType,
                requestHost,
                sourceHost
            )
    }
}
