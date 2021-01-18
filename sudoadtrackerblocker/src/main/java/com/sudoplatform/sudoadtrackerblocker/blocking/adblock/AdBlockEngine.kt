/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudoadtrackerblocker.blocking.adblock

import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import java.io.IOException

/**
 * The JNI interface to the Adblock Rust FFI blocking engine.
 *
 * @since 2020-11-20
 */
@Keep
internal class AdBlockEngine : AutoCloseable {

    companion object {
        init {
            // Load the JNI library that wraps the adblock-rust-ffi library
            System.loadLibrary("adblockjni")
        }
    }

    /** Storage for the pointer to the vector of Adblock engines  */
    private var engines: Long = 0

    /**
     * Load a set of EasyList or EasyPrivacy URL blocking rules into the adblock engines.
     *
     * @param rules EasyList, EasyPrivacy or Fanboy Social URL blocking rules
     */
    external fun loadRules(rules: String)

    /**
     * Clear all the rules from the adblock engines.
     */
    external fun clearRules()

    /**
     * Check if the resourceUrl should be allowed to be loaded based on the rules loaded
     * into the engines.
     *
     * @param resourceUrl The resource being requested to load
     * @param sourceUrl The URL of the page from which the request comes
     * @param resourceType The MIME type of the resource being loaded, pass null if unknown
     * @param requestHost The hostname from in the resourceUrl
     * @param sourceHost The hostname from the sourceUrl
     * @return true if the resourceUrl is not blocked by the loaded rules, false if it is blocked.
     */
    external fun shouldLoad(
        resourceUrl: String,
        sourceUrl: String,
        resourceType: String? = null,
        requestHost: String,
        sourceHost: String
    ): Boolean

    /**
     * Expose the native code domainResolver method to Java/Kotlin to make it easy to test.
     *
     * @param url The URL from which to find the domain name
     * @return The domain name extracted from the URL or null.
     */
    @VisibleForTesting
    external fun domainResolver(url: String): String

    @Throws(IOException::class)
    override fun close() {
        clearRules()
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        close()
    }
}
