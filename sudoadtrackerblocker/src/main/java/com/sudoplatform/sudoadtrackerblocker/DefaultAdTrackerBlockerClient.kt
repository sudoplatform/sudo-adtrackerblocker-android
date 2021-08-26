/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudoadtrackerblocker

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.amazonaws.services.cognitoidentity.model.NotAuthorizedException
import com.sudoplatform.sudoadtrackerblocker.blocking.BlockingExceptions
import com.sudoplatform.sudoadtrackerblocker.blocking.BlockingProvider
import com.sudoplatform.sudoadtrackerblocker.blocking.DefaultBlockingProvider
import com.sudoplatform.sudoadtrackerblocker.s3.DefaultS3Client
import com.sudoplatform.sudoadtrackerblocker.s3.S3Client
import com.sudoplatform.sudoadtrackerblocker.s3.S3Exception
import com.sudoplatform.sudoadtrackerblocker.storage.StorageProvider
import com.sudoplatform.sudoadtrackerblocker.transformers.ExceptionsTransformer
import com.sudoplatform.sudoadtrackerblocker.transformers.RulesetTransformer
import com.sudoplatform.sudoadtrackerblocker.types.BlockingException
import com.sudoplatform.sudoadtrackerblocker.types.Ruleset
import com.sudoplatform.sudoadtrackerblocker.types.allRulesets
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.exceptions.AuthenticationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import java.io.IOException
import java.util.Date
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

/**
 * The default implementation of [SudoAdTrackerBlockerClient] provided by this SDK.
 *
 * @since 2020-11-12
 */
internal class DefaultAdTrackerBlockerClient(
    context: Context,
    private val logger: Logger,
    sudoUserClient: SudoUserClient,
    private val region: String,
    private val bucket: String,
    private val storageProvider: StorageProvider,
    @VisibleForTesting
    private val s3Client: S3Client = DefaultS3Client(context, sudoUserClient, region, bucket, logger),
    @VisibleForTesting
    private val blockingProvider: BlockingProvider = DefaultBlockingProvider(logger),
    override val ENTITLEMENT_NAME: String = "sudoplatform.atb.atbUserEntitled"
) : SudoAdTrackerBlockerClient, CoroutineScope {

    companion object {
        /** Deny lists file names and paths */
        @VisibleForTesting
        internal const val ADS_FILE = "easylist.txt"

        @VisibleForTesting
        internal const val PRIVACY_FILE = "easyprivacy.txt"

        @VisibleForTesting
        internal const val SOCIAL_FILE = "fanboy-social.txt"

        @VisibleForTesting
        internal const val ADS_SUBPATH = "adblock-plus/AD"

        @VisibleForTesting
        internal const val PRIVACY_SUBPATH = "adblock-plus/PRIVACY"

        @VisibleForTesting
        internal const val SOCIAL_SUBPATH = "adblock-plus/SOCIAL"

        @VisibleForTesting
        internal const val S3_TOP_PATH = "/filter-lists"

        private val EPOCH = Date(0L)

        /** Exception messages */
        private const val ERROR_NOT_IMPLEMENTED = "Not yet implemented"
    }

    override val coroutineContext: CoroutineContext = Dispatchers.IO

    @VisibleForTesting
    internal var actualStatus = SudoAdTrackerBlockerClient.FilterEngineStatus.UNKNOWN

    @VisibleForTesting
    internal var blockingProviderError: Throwable? = null
    private val blockingExceptions = BlockingExceptions(storageProvider)

    private val deferredSetup: Deferred<Unit>

    init {
        deferredSetup = async {
            setupBlockingProvider()
        }
    }

    override val status: SudoAdTrackerBlockerClient.FilterEngineStatus
        get() = actualStatus

    override suspend fun listRulesets(): List<Ruleset> {
        try {
            return RulesetTransformer.toRulesetList(
                s3Client.list(path = S3_TOP_PATH)
            )
        } catch (e: Throwable) {
            logger.debug("Error $e")
            throw interpretException(e)
        }
    }

    private fun Ruleset.Type.toPathAndFileName(): Pair<String, String>? {
        return when (this) {
            Ruleset.Type.AD_BLOCKING -> Pair(ADS_SUBPATH, ADS_FILE)
            Ruleset.Type.PRIVACY -> Pair(PRIVACY_SUBPATH, PRIVACY_FILE)
            Ruleset.Type.SOCIAL -> Pair(SOCIAL_SUBPATH, SOCIAL_FILE)
            else -> null
        }
    }

    private fun makeS3Path(path: String, fileName: String): String {
        return "$S3_TOP_PATH/$path/$fileName"
    }

    override suspend fun getActiveRulesets(): Array<Ruleset.Type> {
        try {
            val preferences = storageProvider.readPreferences()
            return allRulesets().filter {
                true.toString() == preferences[it.name]
            }.toTypedArray()
        } catch (e: Throwable) {
            logger.debug("Error $e")
            throw interpretException(e)
        }
    }

    override suspend fun setActiveRulesets(activeRuleset: Ruleset.Type, vararg moreActiveRulesets: Ruleset.Type) {
        setActiveRulesets(arrayOf(activeRuleset, *moreActiveRulesets))
    }

    override suspend fun setActiveRulesets(activeRulesets: Array<Ruleset.Type>) {
        try {
            val status = mutableMapOf<String, String>()
            activeRulesets.forEach { rulesetType ->
                status[rulesetType.name] = true.toString()
            }
            storageProvider.writePreferences(status.toMap())

            // Reinitialize the engines with the new rules
            setupBlockingProvider()
        } catch (e: Throwable) {
            logger.debug("Error $e")
            throw interpretException(e)
        }
    }

    override suspend fun getExceptions(): Set<BlockingException> {
        try {
            return blockingExceptions.readExceptions()
        } catch (e: Throwable) {
            logger.debug("Error $e")
            throw interpretException(e)
        }
    }

    override suspend fun addExceptions(vararg exceptions: BlockingException) {
        try {
            if (exceptions.isEmpty()) {
                return
            }

            val exceptionsSet = blockingExceptions.readExceptions().toMutableSet()
            exceptionsSet.addAll(exceptions)
            blockingExceptions.writeExceptions(exceptionsSet)

            // Reinitialize the engines with the new rules
            setupBlockingProvider()
        } catch (e: Throwable) {
            logger.debug("Error $e")
            throw interpretException(e)
        }
    }

    override suspend fun removeExceptions(vararg exceptions: BlockingException) {
        try {
            if (exceptions.isEmpty()) {
                return
            }

            val exceptionsSet = blockingExceptions.readExceptions().toMutableSet()
            exceptionsSet.removeAll(exceptions)
            blockingExceptions.writeExceptions(exceptionsSet)

            // Reinitialize the engines with the new rules
            setupBlockingProvider()
        } catch (e: Throwable) {
            logger.debug("Error $e")
            throw interpretException(e)
        }
    }

    override suspend fun removeAllExceptions() {
        try {
            blockingExceptions.deleteExceptions()

            // Reinitialize the engines with the new rules
            setupBlockingProvider()
        } catch (e: Throwable) {
            logger.debug("Error $e")
            throw interpretException(e)
        }
    }

    override suspend fun updateRulesets(vararg rulesetTypes: Ruleset.Type) {
        try {
            val supportedRulesets = rulesetTypes.filter { it != Ruleset.Type.UNKNOWN }
            if (supportedRulesets.isEmpty()) {
                return
            }
            listRulesets().filter { ruleset ->
                ruleset.type in supportedRulesets
            }.forEach { ruleset ->
                val (subPath, fileName) = ruleset.type.toPathAndFileName()
                    ?: run {
                        logger.debug("Unsupported ruleset ${ruleset.type} requested")
                        return@forEach
                    }
                val localETag = storageProvider.readFileETag(fileName)
                if (ruleset.eTag != localETag) {
                    // eTag from the service is different to what we have locally, this
                    // means the rules have been updated on the backend
                    val s3Path = makeS3Path(subPath, fileName)
                    s3Client.download(s3Path).also { rulesetBytes ->
                        storageProvider.write(fileName, rulesetBytes)
                        storageProvider.writeFileETag(fileName, ruleset.eTag)
                    }
                }
            }
        } catch (e: S3Exception.DownloadException) {
            logger.debug("Ruleset not found $e")
        } catch (e: Throwable) {
            logger.debug("Error $e")
            throw interpretException(e)
        }
    }

    override suspend fun clearStorage() {
        close()
        try {
            storageProvider.deleteFiles()
            storageProvider.deleteFileETags()
            storageProvider.deletePreferences()
        } catch (e: Throwable) {
            logger.debug("Error $e")
            throw interpretException(e)
        }
    }

    override fun close() {
        try {
            deferredSetup.cancel()
            blockingProvider.close()
            coroutineContext.cancelChildren()
            coroutineContext.cancel()
        } catch (e: CancellationException) {
            // Never suppress this exception it's used by coroutines to cancel outstanding work
            throw e
        } catch (e: Throwable) {
            // Suppress and log anything bad that happened while closing
            logger.warning("Error while closing $e")
        }
    }

    override suspend fun checkUrl(url: String, sourceUrl: String?, resourceType: String?): SudoAdTrackerBlockerClient.CheckUrlResult {
        if (status != SudoAdTrackerBlockerClient.FilterEngineStatus.READY) {
            throw SudoAdTrackerBlockerException.FilterEngineNotReadyException("Filtering engine not ready, status is $status")
        }
        try {
            if (blockingProvider.checkIsUrlBlocked(url, sourceUrl, resourceType)) {
                return SudoAdTrackerBlockerClient.CheckUrlResult.BLOCKED
            } else {
                return SudoAdTrackerBlockerClient.CheckUrlResult.ALLOWED
            }
        } catch (e: Throwable) {
            logger.debug("Error $e")
            throw interpretException(e)
        }
    }

    private suspend fun setupBlockingProvider() {
        try {
            logger.info("Starting blocking initialization.")
            actualStatus = SudoAdTrackerBlockerClient.FilterEngineStatus.PREPARING

            blockingProvider.close()

            val activeRulesets = getActiveRulesetsMetadata()
            if (activeRulesets.isNotEmpty()) {
                val activeRulesetTypes = activeRulesets.map { it.type.name }
                logger.info("Initializing blocking for $activeRulesetTypes.")

                val activeRules = activeRulesets.mapNotNull { ruleset ->
                    getRules(ruleset.type)
                }

                // Create exception rules from the exception list
                val exceptions = blockingExceptions.readExceptions()
                logger.info("Loaded exception list with ${exceptions.size} entries.")
                val exceptionRules = ExceptionsTransformer.toExceptionRules(exceptions)

                blockingProvider.setRules(activeRules, exceptionRules)
                logger.info("Blocking initialization completed successfully.")
            } else {
                logger.info("Initialization skipped, there are no active rulesets.")
            }
            actualStatus = SudoAdTrackerBlockerClient.FilterEngineStatus.READY
        } catch (e: CancellationException) {
            // Never suppress this exception it's used by coroutines to cancel outstanding work
            actualStatus = SudoAdTrackerBlockerClient.FilterEngineStatus.UNKNOWN
            throw e
        } catch (e: Throwable) {
            logger.outputError(Error(e))
            logger.error("Blocking initialization failed $e")
            actualStatus = SudoAdTrackerBlockerClient.FilterEngineStatus.ERROR
            blockingProviderError = interpretException(e)
        }
    }

    private fun getRules(rulesetType: Ruleset.Type): ByteArray? {
        val (_, fileName) = rulesetType.toPathAndFileName()
            ?: run {
                logger.debug("Unsupported ruleset $rulesetType requested")
                return null
            }
        return storageProvider.read(fileName)
            ?: throw SudoAdTrackerBlockerException.NoSuchRulesetException(
                "Ruleset $rulesetType has not been downloaded, please call updateRulesets first."
            )
    }

    private suspend fun getActiveRulesetsMetadata(): List<Ruleset> {
        val activeRulesetTypes = getActiveRulesets()
        if (activeRulesetTypes.isEmpty()) {
            // Avoid unnecessarily querying S3 in listRulesets below if no ruleset types are active.
            return emptyList()
        }
        return listRulesets().filter { ruleset ->
            ruleset.type in activeRulesetTypes
        }
    }

    /**
     * Interpret an exception from SudoUserClient, S3 or the AdBlockEngine and map it to an exception
     * declared in this SDK's API that the caller is expecting.
     *
     * @param exception The exception from the secure value client.
     * @return The exception mapped to [SudoAdTrackerBlockerException]
     * or [CancellationException]
     */
    private fun interpretException(exception: Throwable): Throwable {
        return when (exception) {
            is CancellationException, // Never wrap or reinterpret Kotlin coroutines cancellation exception
            is SudoAdTrackerBlockerException -> exception
            is S3Exception.MetadataException -> SudoAdTrackerBlockerException.DataFormatException(cause = exception)
            is S3Exception -> throw SudoAdTrackerBlockerException.FailedException(cause = exception)
            is NotAuthorizedException, is AuthenticationException.NotAuthorizedException ->
                throw SudoAdTrackerBlockerException.UnauthorizedUserException(cause = exception)
            is IOException -> throw SudoAdTrackerBlockerException.FailedException(cause = exception)
            else -> SudoAdTrackerBlockerException.UnknownException(exception)
        }
    }
}
