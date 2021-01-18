/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sudoplatform.sudoadtrackerblocker.TestData.ADVERTISERS
import com.sudoplatform.sudoadtrackerblocker.TestData.PRIVACY_VIOLATORS
import com.sudoplatform.sudoadtrackerblocker.TestData.SHOULD_NOT_BE_BLOCKED
import com.sudoplatform.sudoadtrackerblocker.storage.DefaultStorageProvider
import com.sudoplatform.sudoadtrackerblocker.types.Ruleset
import com.sudoplatform.sudoadtrackerblocker.types.allRulesets
import com.sudoplatform.sudoadtrackerblocker.types.noRulesets
import com.sudoplatform.sudoadtrackerblocker.types.toHostException
import com.sudoplatform.sudoadtrackerblocker.types.toPageException
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test the operation of the [SudoAdTrackerBlockerClient].
 *
 * @since 2020-11-12
 */
@RunWith(AndroidJUnit4::class)
class SudoAdTrackerBlockerClientIntegrationTest : BaseIntegrationTest() {

    private var adTrackerBlockerClient: SudoAdTrackerBlockerClient? = null
    private val storageProvider = DefaultStorageProvider(context)

    @Before
    fun init() {
        Timber.plant(Timber.DebugTree())
    }

    @After
    fun fini() = runBlocking<Unit> {
        if (clientConfigFilesPresent()) {
            adTrackerBlockerClient?.clearStorage()
        }
        Timber.uprootAll()
    }

    private fun createClient() = runBlocking<SudoAdTrackerBlockerClient> {
        adTrackerBlockerClient = SudoAdTrackerBlockerClient.builder()
            .setContext(context)
            .setSudoUserClient(userClient)
            .setStorageProvider(storageProvider)
            .setLogger(logger)
            .build()
            .apply {
                waitForClientInitToComplete(this)
            }
        adTrackerBlockerClient!!.clearStorage()
        adTrackerBlockerClient!!
    }

    @Test
    fun shouldThrowIfRequiredItemsNotProvidedToBuilder() {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        // All required items not provided
        shouldThrow<NullPointerException> {
            SudoAdTrackerBlockerClient.builder().build()
        }

        // Context not provided
        shouldThrow<NullPointerException> {
            SudoAdTrackerBlockerClient.builder()
                .setSudoUserClient(userClient)
                .build()
        }

        // SudoUserClient not provided
        shouldThrow<NullPointerException> {
            SudoAdTrackerBlockerClient.builder()
                .setContext(context)
                .build()
        }
    }

    @Test
    fun shouldNotThrowIfTheRequiredItemsAreProvidedToBuilder() {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        SudoAdTrackerBlockerClient.builder()
            .setContext(context)
            .setSudoUserClient(userClient)
            .build()
    }

    @Test
    fun shouldBeAbleToActivateRulesets() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())
        signInAndRegisterUser()
        val client = createClient()

        val rulesets = client.listRulesets()
        rulesets shouldHaveSize 3

        storageProvider.listFiles() shouldHaveSize 0

        val adBlockRuleset = rulesets.find { it.type == Ruleset.Type.AD_BLOCKING }
        adBlockRuleset shouldNotBe null
        with(adBlockRuleset!!) {
            id shouldNotBe ""
            eTag shouldNotBe ""
            type shouldBe Ruleset.Type.AD_BLOCKING
            updatedAt.time shouldBeGreaterThan 0L
        }
        client.updateRulesets(Ruleset.Type.AD_BLOCKING)
        storageProvider.listFiles() shouldContainExactlyInAnyOrder listOf(
            "easylist.txt"
        )

        val privacyRuleset = rulesets.find { it.type == Ruleset.Type.PRIVACY }
        privacyRuleset shouldNotBe null
        with(privacyRuleset!!) {
            id shouldNotBe ""
            eTag shouldNotBe ""
            type shouldBe Ruleset.Type.PRIVACY
            updatedAt.time shouldBeGreaterThan 0L
        }
        client.updateRulesets(Ruleset.Type.PRIVACY)
        storageProvider.listFiles() shouldContainExactlyInAnyOrder listOf(
            "easylist.txt", "easyprivacy.txt"
        )

        val socialRuleset = rulesets.find { it.type == Ruleset.Type.SOCIAL }
        socialRuleset shouldNotBe null
        with(socialRuleset!!) {
            id shouldNotBe ""
            eTag shouldNotBe ""
            type shouldBe Ruleset.Type.SOCIAL
            updatedAt.time shouldBeGreaterThan 0L
        }
        client.updateRulesets(Ruleset.Type.SOCIAL)
        storageProvider.listFiles() shouldContainExactlyInAnyOrder listOf(
            "easylist.txt", "easyprivacy.txt", "fanboy-social.txt"
        )

        client.clearStorage()
        storageProvider.listFiles() shouldHaveSize 0

        client.updateRulesets()
        storageProvider.listFiles() shouldHaveSize 3
    }

    /**
     * Test the happy path of ad tracker blocker operations, which is the normal flow a
     * user would be expected to exercise.
     */
    @Test
    fun completeFlowShouldSucceed() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())
        signInAndRegisterUser()
        val client = createClient()

        val rulesets = client.listRulesets()
        rulesets shouldHaveSize 3

        client.updateRulesets()
        client.setActiveRulesets(allRulesets())
        val currentUrl = "http://somehost.eu/contact"
        for (testCase in PRIVACY_VIOLATORS.keys) {
            isBlocked(testCase.toUrl(), currentUrl) shouldBe true
        }
        for (testCase in ADVERTISERS.keys) {
            isBlocked(testCase.toUrl(), currentUrl) shouldBe true
        }
        for (testCase in SHOULD_NOT_BE_BLOCKED.keys) {
            isBlocked(testCase.toUrl(), currentUrl) shouldBe false
        }

        // Add the host of the current URL to the exception list and verify they are no longer blocked
        client.removeAllExceptions()
        client.addExceptions(toHostException(currentUrl))
        for (testCase in PRIVACY_VIOLATORS.keys) {
            isBlocked(testCase.toUrl(), currentUrl) shouldBe false
        }
        for (testCase in ADVERTISERS.keys) {
            isBlocked(testCase.toUrl(), currentUrl) shouldBe false
        }
        for (testCase in SHOULD_NOT_BE_BLOCKED.keys) {
            isBlocked(testCase.toUrl(), currentUrl) shouldBe false
        }

        // Add the page of the current URL to the exception list and verify they are no longer blocked
        client.removeAllExceptions()
        client.addExceptions(toPageException(currentUrl))
        for (testCase in PRIVACY_VIOLATORS.keys) {
            isBlocked(testCase.toUrl(), currentUrl) shouldBe false
        }
        for (testCase in ADVERTISERS.keys) {
            isBlocked(testCase.toUrl(), currentUrl) shouldBe false
        }
        for (testCase in SHOULD_NOT_BE_BLOCKED.keys) {
            isBlocked(testCase.toUrl(), currentUrl) shouldBe false
        }

        // Pass in a blank currentUrl and verify they are all blocked
        client.removeAllExceptions()
        client.addExceptions(toPageException(currentUrl))
        for (testCase in PRIVACY_VIOLATORS.keys) {
            isBlocked(testCase.toUrl(), "") shouldBe true
        }
        for (testCase in ADVERTISERS.keys) {
            isBlocked(testCase.toUrl(), "") shouldBe true
        }
        for (testCase in SHOULD_NOT_BE_BLOCKED.keys) {
            isBlocked(testCase.toUrl(), "") shouldBe false
        }

        // Remove all the exceptions and verify the original behaviour
        client.removeAllExceptions()
        for (testCase in PRIVACY_VIOLATORS.keys) {
            isBlocked(testCase.toUrl(), currentUrl) shouldBe true
        }
        for (testCase in ADVERTISERS.keys) {
            isBlocked(testCase.toUrl(), currentUrl) shouldBe true
        }
        for (testCase in SHOULD_NOT_BE_BLOCKED.keys) {
            isBlocked(testCase.toUrl(), currentUrl) shouldBe false
        }
    }

    private suspend fun isBlocked(url: String, currentUrl: String): Boolean {
        return adTrackerBlockerClient!!.checkUrl(
            url,
            currentUrl,
            "script"
        ) == SudoAdTrackerBlockerClient.CheckUrlResult.BLOCKED
    }

    @Test
    fun checkActiveRulesets() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())
        signInAndRegisterUser()
        val client = createClient()

        client.getActiveRulesets() shouldBe noRulesets()

        allRulesets().forEach { rulesetType ->
            client.setActiveRulesets(rulesetType)
            client.getActiveRulesets() shouldBe arrayOf(rulesetType)
        }

        client.setActiveRulesets(allRulesets())
        client.getActiveRulesets() shouldBe allRulesets()

        client.clearStorage()
        client.getActiveRulesets() shouldBe emptyArray()
    }

    fun shouldNotBeAbleToAddDuplicateExceptions() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())
        signInAndRegisterUser()
        val client = createClient()

        val hostException = toHostException("http://sudoplatform.com")
        val pageException = toPageException("https://docs.sudoplatform.com/guides/getting-started")

        client.removeAllExceptions()
        client.addExceptions(hostException, pageException)
        client.getExceptions() shouldHaveSize 2

        client.addExceptions(pageException, hostException, pageException, hostException)
        client.getExceptions() shouldHaveSize 2

        client.removeExceptions(pageException, hostException)
        client.getExceptions() shouldHaveSize 0
    }

    @Test
    fun checkExceptionListHandling() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())
        signInAndRegisterUser()
        val client = createClient()

        client.getExceptions() shouldHaveSize 0

        val hostException = toHostException("foo.com")
        client.addExceptions(hostException)
        client.getExceptions() shouldContainExactlyInAnyOrder setOf(hostException)

        val pageException1 = toHostException("ccc.com/about")
        client.addExceptions(pageException1)
        client.getExceptions() shouldContainExactlyInAnyOrder setOf(hostException, pageException1)

        client.removeExceptions(pageException1)
        client.getExceptions() shouldContainExactlyInAnyOrder setOf(hostException)

        val pageException2 = toHostException("ddd.com/foo")
        client.addExceptions(pageException2)
        client.getExceptions() shouldContainExactlyInAnyOrder setOf(hostException, pageException2)

        client.removeAllExceptions()
        client.getExceptions() shouldHaveSize 0
    }
}
