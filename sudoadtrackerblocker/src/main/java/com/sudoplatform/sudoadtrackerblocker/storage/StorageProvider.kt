/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudoadtrackerblocker.storage

import androidx.annotation.VisibleForTesting
import java.io.IOException

/**
 * Storage services are provided to the [SudoAdTrackerBlockerClient] by classes that implement
 * this interface.
 *
 * @since 2020-11-19
 */
interface StorageProvider {

    /**
     * Reads all the bytes from a file.
     *
     * @param fileName The name of the file without a path.
     * @return The contents of the file or null if the file does not exist.
     */
    @Throws(IOException::class)
    fun read(fileName: String): ByteArray?

    /**
     * Writes all the bytes to a file.
     *
     * @param fileName The name of the file without a path.
     * @param data The contents of the file.
     */
    @Throws(IOException::class)
    fun write(fileName: String, data: ByteArray)

    /**
     * Delete a file.
     *
     * @param fileName The name of the file without a path.
     * @return true if the file was deleted, false if it didn't exist
     */
    @Throws(IOException::class)
    fun delete(fileName: String): Boolean

    /**
     * Delete all the files managed by the [StorageProvider].
     */
    @Throws(IOException::class)
    fun deleteFiles()

    /**
     * List all the files in the storage provider.
     *
     * @return [List] of file names.
     */
    @VisibleForTesting
    @Throws(IOException::class)
    fun listFiles(): List<String>

    /**
     * Reads the eTag of a file, returns null if the file does not exist.
     *
     * @param fileName The name of the file without a path.
     * @return The eTag of the file or null if the file does not exist.
     */
    @Throws(IOException::class)
    fun readFileETag(fileName: String): String?

    /**
     * Writes the eTag of a file.
     *
     * @param fileName The name of the file without a path.
     * @param eTag The eTag of the file.
     */
    @Throws(IOException::class)
    fun writeFileETag(fileName: String, eTag: String)

    /**
     * Delete the eTag of a file.
     *
     * @param fileName The name of the file without a path.
     * @return true if the file's eTag was deleted, false if it didn't exist
     */
    @Throws(IOException::class)
    fun deleteFileETag(fileName: String): Boolean

    /**
     * Delete all the file eTags managed by the [StorageProvider].
     */
    @Throws(IOException::class)
    fun deleteFileETags()

    /**
     * Reads the preferences from storage. Preference storage might be restricted
     * by implementations to a maximum size. It should not be considered appropriate
     * to store large amounts of data in the preferences.
     *
     * @return [Map] of the preferences.
     */
    @Throws(IOException::class)
    fun readPreferences(): Map<String, String>

    /**
     * Writes the preferences to storage. Preference storage might be restricted
     * by implementations to a maximum size. It should not be considered appropriate
     * to store large amounts of data in the preferences.
     *
     * @param preferences [Map] of the preferences.
     */
    @Throws(IOException::class)
    fun writePreferences(preferences: Map<String, String>)

    /**
     * Delete the set of preferences.
     */
    @Throws(IOException::class)
    fun deletePreferences()
}
