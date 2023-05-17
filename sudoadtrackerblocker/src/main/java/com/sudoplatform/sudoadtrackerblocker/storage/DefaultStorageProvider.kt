/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.storage

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private const val PACKAGE = "com.sudoplatform.sudoadtrackerblocker"

/**
 * Directory in which cached rules files will be stored. This is a value that shouldn't clash with
 * one chosen by the consuming app.
 */
private const val CACHE_SUBDIR = "$PACKAGE.cache"

/**
 * Directory in which cached file eTags will be stored. This is a value that shouldn't clash with
 * one chosen by the consuming app.
 */
private const val ETAG_SUBDIR = "$PACKAGE.etag"

/**
 * The private set of [SharedPreferences] used to store these preferences.
 */
private const val PREFERENCE_SET_NAME = "$PACKAGE.preferences"

/**
 * A string set preference in which the keys of the callers preferences are stored
 * to make it easy to fetch all the current preferences.
 */
private const val KEY_SET_NAME = "$PACKAGE.keys"

/**
 * Default implementation of [StorageProvider] that uses private local storage.
 *
 * @since 2020-11-25
 */
internal class DefaultStorageProvider(private val context: Context) : StorageProvider {

    private val cacheDir = File(context.cacheDir, CACHE_SUBDIR)
    private val eTagDir = File(context.cacheDir, ETAG_SUBDIR)

    private fun ensureDirsExist() {
        if (!cacheDir.exists()) {
            cacheDir.mkdir()
        }
        if (!eTagDir.exists()) {
            eTagDir.mkdir()
        }
    }

    private fun getFile(fileName: String) = File(cacheDir, fileName)

    override fun read(fileName: String): ByteArray? {
        ensureDirsExist()
        val file = getFile(fileName)
        if (file.exists() && file.canRead()) {
            return file.readBytes()
        }
        return null
    }

    override fun write(fileName: String, data: ByteArray) {
        ensureDirsExist()
        getFile(fileName).writeBytes(data)
    }

    override fun delete(fileName: String): Boolean {
        val file = getFile(fileName)
        if (file.exists()) {
            return file.delete()
        }
        return false
    }

    override fun deleteFiles() {
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
            cacheDir.mkdir()
        }
    }

    override fun listFiles(): List<String> {
        if (cacheDir.exists()) {
            return cacheDir.listFiles()
                ?.filter { it.isFile }
                ?.map { it.name }
                ?: emptyList()
        }
        return emptyList()
    }

    private fun getETagFile(fileName: String) = File(eTagDir, fileName)

    override fun readFileETag(fileName: String): String? {
        ensureDirsExist()
        val eTagFile = getETagFile(fileName)
        if (eTagFile.exists() && eTagFile.canRead()) {
            FileInputStream(eTagFile).bufferedReader().use { reader ->
                return reader.readText().trim()
            }
        }
        return null
    }

    override fun writeFileETag(fileName: String, eTag: String) {
        ensureDirsExist()
        FileOutputStream(getETagFile(fileName)).bufferedWriter().use { writer ->
            writer.write(eTag.trim())
        }
    }

    override fun deleteFileETag(fileName: String): Boolean {
        val eTagFile = getETagFile(fileName)
        if (eTagFile.exists()) {
            return eTagFile.delete()
        }
        return false
    }

    override fun deleteFileETags() {
        if (eTagDir.exists()) {
            eTagDir.deleteRecursively()
            eTagDir.mkdir()
        }
    }

    // The keys of the preferences the caller supplies are stored in the string set
    // so that the entire set of preferences can be read and they can use whatever keys they like
    override fun readPreferences(): Map<String, String> {
        val preferences = mutableMapOf<String, String>()
        with(context.getSharedPreferences(PREFERENCE_SET_NAME, MODE_PRIVATE)) {
            getStringSet(KEY_SET_NAME, emptySet())?.forEach { key ->
                getString(key, null)?.let { value ->
                    preferences.put(key, value)
                }
            }
        }
        return preferences
    }

    // The keys of the preferences the caller supplies are stored in the string set
    // so that the entire set of preferences can be read and they can use whatever keys they like
    override fun writePreferences(preferences: Map<String, String>) {
        with(context.getSharedPreferences(PREFERENCE_SET_NAME, MODE_PRIVATE)) {
            val oldKeys = getStringSet(KEY_SET_NAME, emptySet())
            edit {
                oldKeys?.forEach { key ->
                    remove(key)
                }
                preferences.keys.forEach { key ->
                    putString(key, preferences[key])
                }
                putStringSet(KEY_SET_NAME, preferences.keys)
                apply()
            }
        }
    }

    override fun deletePreferences() {
        with(context.getSharedPreferences(PREFERENCE_SET_NAME, MODE_PRIVATE)) {
            val oldKeys = getStringSet(KEY_SET_NAME, emptySet())
            edit {
                oldKeys?.forEach { key ->
                    remove(key)
                }
                remove(KEY_SET_NAME)
                apply()
            }
        }
    }
}
