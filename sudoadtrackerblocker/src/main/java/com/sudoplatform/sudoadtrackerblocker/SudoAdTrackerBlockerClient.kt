/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudoadtrackerblocker

import android.content.Context
import com.sudoplatform.sudoadtrackerblocker.logging.LogConstants
import com.sudoplatform.sudoadtrackerblocker.s3.readS3Configuration
import com.sudoplatform.sudoadtrackerblocker.storage.DefaultStorageProvider
import com.sudoplatform.sudoadtrackerblocker.storage.StorageProvider
import com.sudoplatform.sudoadtrackerblocker.types.BlockingException
import com.sudoplatform.sudoadtrackerblocker.types.Ruleset
import com.sudoplatform.sudoadtrackerblocker.types.allRulesets
import com.sudoplatform.sudoadtrackerblocker.types.noRulesets
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudouser.SudoUserClient
import java.util.Objects

/**
 * Interface encapsulating a library for interacting with the Sudo Ad Tracker Blocker service.
 * @sample com.sudoplatform.sudoadtrackerblocker.samples.Samples.buildClient
 *
 * @since 2020-11-17
 */
interface SudoAdTrackerBlockerClient : AutoCloseable {

    companion object {
        /** Create a [Builder] for [SudoAdTrackerBlockerClient]. */
        @JvmStatic
        fun builder() = Builder()
    }

    /**
     * Builder used to construct the [SudoAdTrackerBlockerClient].
     */
    class Builder internal constructor() {
        private var context: Context? = null
        private var sudoUserClient: SudoUserClient? = null
        private var logger: Logger = Logger(LogConstants.SUDOLOG_TAG, AndroidUtilsLogDriver(LogLevel.INFO))
        private var storageProvider: StorageProvider? = null

        /**
         * Provide the application context (required input).
         */
        fun setContext(context: Context) = also {
            it.context = context
        }

        /**
         * Provide the implementation of the [SudoUserClient] used to perform
         * sign in and ownership operations (required input).
         */
        fun setSudoUserClient(sudoUserClient: SudoUserClient) = also {
            it.sudoUserClient = sudoUserClient
        }

        /**
         * Provide the implementation of the [StorageProvider] used to read and write cached
         * metadata and contents and the allow list (optional input). If a value is not supplied
         * a default implementation will be used.
         */
        fun setStorageProvider(storageProvider: StorageProvider) = also {
            it.storageProvider = storageProvider
        }

        /**
         * Provide the implementation of the [Logger] used for logging errors (optional input).
         * If a value is not supplied a default implementation will be used.
         */
        fun setLogger(logger: Logger) = also {
            it.logger = logger
        }

        /**
         * Construct the [SudoAdTrackerBlockerClient]. Will throw a [NullPointerException] if
         * the [context] or [sudoUserClient] have not been provided or [ConfigurationException]
         * if the sudoplatformconfig.json file is missing the region or bucket item in the
         * identityService stanza.
         */
        @Throws(NullPointerException::class, SudoAdTrackerBlockerException.ConfigurationException::class)
        fun build(): SudoAdTrackerBlockerClient {
            Objects.requireNonNull(context, "Context must be provided.")
            Objects.requireNonNull(sudoUserClient, "SudoUserClient must be provided.")

            val (region, bucket) = readS3Configuration(context!!, logger)

            return DefaultAdTrackerBlockerClient(
                context = context!!,
                sudoUserClient = sudoUserClient!!,
                logger = logger,
                region = region,
                bucket = bucket,
                storageProvider = storageProvider ?: DefaultStorageProvider(context!!)
            )
        }
    }

    /**
     * List the available [Ruleset]s from the service.
     *
     * @return a [List] of [Ruleset]s that are available to be used.
     * @sample com.sudoplatform.sudoadtrackerblocker.samples.Samples.listRulesets
     */
    @Throws(SudoAdTrackerBlockerException::class)
    suspend fun listRulesets(): List<Ruleset>

    /**
     * Returns the active [Ruleset]s.
     *
     * @return [Array] containing only the active ruleset types.
     * @sample com.sudoplatform.sudoadtrackerblocker.samples.Samples.getActiveRulesets
     */
    @Throws(SudoAdTrackerBlockerException::class)
    suspend fun getActiveRulesets(): Array<Ruleset.Type>

    /**
     * Sets which [Ruleset]s are active. This method does not return until the new rules
     * have been compiled and the filtering engine is ready. [updateRulesets] must be
     * called to update the local copy of the [Ruleset]s before this method is called
     * for the very first time.
     *
     * @param activeRuleset The [Ruleset] that should become active.
     * @param moreActiveRulesets The other [Ruleset]s that should become active.
     * @sample com.sudoplatform.sudoadtrackerblocker.samples.Samples.setActiveRulesets
     */
    @Throws(SudoAdTrackerBlockerException::class)
    suspend fun setActiveRulesets(
        activeRuleset: Ruleset.Type,
        vararg moreActiveRulesets: Ruleset.Type = emptyArray()
    )

    /**
     * Sets which [Ruleset]s are active. This method does not return until the new rules
     * have been compiled and the filtering engine is ready. [updateRulesets] must be
     * called to update the local copy of the [Ruleset]s before this method is called
     * for the very first time.
     *
     * @param activeRulesets The [Ruleset]s that should become active. All [Ruleset]s will be
     * deactivated if this argument is not supplied.
     * @sample com.sudoplatform.sudoadtrackerblocker.samples.Samples.setActiveRulesets
     */
    @Throws(SudoAdTrackerBlockerException::class)
    suspend fun setActiveRulesets(activeRulesets: Array<Ruleset.Type> = noRulesets())

    /**
     * Request the [Ruleset]s are updated from the service. This method must be called
     * to update the local copy of the [Ruleset]s before any [Ruleset]s are made active
     * by a call to [setActiveRulesets].
     *
     * @param rulesetTypes The [Ruleset.Type]s to update. All [Ruleset]s will be
     * updated if this argument is not supplied.
     * @sample com.sudoplatform.sudoadtrackerblocker.samples.Samples.updateRulesets
     */
    @Throws(SudoAdTrackerBlockerException::class)
    suspend fun updateRulesets(vararg rulesetTypes: Ruleset.Type = allRulesets())

    /**
     * Gets the set of blocking exceptions that prevent URLs from being blocked.
     * These are the URLs that are to be allowed even if the active rulesets
     * say they should be blocked.
     *
     * @return The exceptions to the blocking rules.
     * @sample com.sudoplatform.sudoadtrackerblocker.samples.Samples.getExceptions
     */
    @Throws(SudoAdTrackerBlockerException::class)
    suspend fun getExceptions(): Set<BlockingException>

    /**
     * Adds an entry to the set of exceptions. This method does not return until the new exceptions
     * have been compiled and the filtering engine is ready.
     *
     * @param exceptions The exceptions to the blocking rules to add.
     * @sample com.sudoplatform.sudoadtrackerblocker.samples.Samples.addExceptions
     */
    @Throws(SudoAdTrackerBlockerException::class)
    suspend fun addExceptions(vararg exceptions: BlockingException)

    /**
     * Removes an entry from the set of exceptions. This method does not return until the new
     * exceptions have been compiled and the filtering engine is ready.
     *
     * @param exceptions The exceptions to the blocking rules to remove.
     * @sample com.sudoplatform.sudoadtrackerblocker.samples.Samples.removeExceptions
     */
    @Throws(SudoAdTrackerBlockerException::class)
    suspend fun removeExceptions(vararg exceptions: BlockingException)

    /**
     * Removes all the entries from the set of exceptions. This method does not return until the
     * new exceptions have been compiled and the filtering engine is ready.
     * @sample com.sudoplatform.sudoadtrackerblocker.samples.Samples.removeAllExceptions
     */
    @Throws(SudoAdTrackerBlockerException::class)
    suspend fun removeAllExceptions()

    enum class FilterEngineStatus {
        /** The filter engine is (re)initializing */
        PREPARING,
        /** The filter engine is ready to be used */
        READY,
        /** The filter engine failed to update or initialize correctly */
        ERROR,
        UNKNOWN
    }

    /** The status of the filter engine. */
    val status: FilterEngineStatus

    enum class CheckUrlResult {
        /** The URL is blocked by the active rulesets and no exception is applicable */
        BLOCKED,
        /** The URL is not blocked and should be used or loaded. */
        ALLOWED
    }

    /**
     * Checks a URL to determine if it is blocked according to current configuration.
     *
     * @param url The URL of the resource that should be checked against the currently active rulesets and the exceptions list
     * @param sourceUrl The URL of the page that requested the [url] be loaded
     * @param resourceType The MIME type of the resource indicated by the [url], null if it is not known
     * @return [CheckUrlResult.BLOCKED] if the URL should not be loaded, [CheckUrlResult.ALLOWED] if it is not blocked.
     * @throws [FilterEngineNotReadyException] if the filtering engine is not ready.
     * @sample com.sudoplatform.sudoadtrackerblocker.samples.Samples.checkUrl
     */
    @Throws(SudoAdTrackerBlockerException::class)
    suspend fun checkUrl(
        url: String,
        sourceUrl: String? = null,
        resourceType: String? = null
    ): CheckUrlResult

    /**
     * Delete all cached data and clear the active [Ruleset]s.
     */
    @Throws(SudoAdTrackerBlockerException::class)
    suspend fun clearStorage()
}
