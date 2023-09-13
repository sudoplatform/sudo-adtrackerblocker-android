/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.blocking

import androidx.annotation.VisibleForTesting
import com.sudoplatform.sudoadtrackerblocker.storage.StorageProvider
import com.sudoplatform.sudoadtrackerblocker.types.BlockingException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Exceptions to the blocking rules are maintained in a set which is stored in a file.
 */
internal class BlockingExceptions(private val storageProvider: StorageProvider) {

    companion object {
        @VisibleForTesting
        internal const val EXCEPTIONS_FILE = "exceptions.txt"

        @VisibleForTesting
        internal const val PAGE_EXCEPTION_SUFFIX = "/"
    }

    fun readExceptions(): Set<BlockingException> {
        val exceptionsBytes = storageProvider.read(EXCEPTIONS_FILE)
            ?: return emptySet()
        val ins = ByteArrayInputStream(exceptionsBytes)
        return ins.bufferedReader().use { reader ->
            reader.readLines().map { line ->
                parseBlockingException(line)
            }
        }.toSet()
    }

    private fun parseBlockingException(s: String): BlockingException {
        var source = s.trim()
        val type: BlockingException.Type
        if (source.trim().endsWith(PAGE_EXCEPTION_SUFFIX)) {
            type = BlockingException.Type.PAGE
            source = source.removeSuffix(PAGE_EXCEPTION_SUFFIX)
        } else {
            type = BlockingException.Type.HOST
        }
        return BlockingException(source, type)
    }

    fun writeExceptions(exceptions: Set<BlockingException>) {
        val out = ByteArrayOutputStream()
        out.bufferedWriter().use { writer ->
            for (exception in exceptions) {
                if (exception.type == BlockingException.Type.PAGE) {
                    writer.append(exception.source).appendLine(PAGE_EXCEPTION_SUFFIX)
                } else {
                    writer.appendLine(exception.source)
                }
            }
        }
        storageProvider.write(EXCEPTIONS_FILE, out.toByteArray())
    }

    fun deleteExceptions() {
        storageProvider.delete(EXCEPTIONS_FILE)
    }
}
