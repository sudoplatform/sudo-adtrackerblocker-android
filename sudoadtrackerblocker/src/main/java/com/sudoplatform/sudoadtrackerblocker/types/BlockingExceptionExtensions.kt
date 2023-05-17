/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker.types

import android.net.Uri
import com.sudoplatform.sudoadtrackerblocker.SudoAdTrackerBlockerException

/**
 * Extension functions to make the API that deal with [BlockingException]s easier to use.
 */

/**
 * Create a [BlockingException] of type [BlockingException.Type.HOST] from a URL.
 *
 * @param url The URL of a web page or host.
 * @return a [BlockingException] of type [BlockingException.Type.HOST] with the [source]
 * set to just the host part of the URL.
 */
fun toHostException(url: String): BlockingException {
    val (host, _) = parse(url)
    return BlockingException(host, BlockingException.Type.HOST)
}

/**
 * Create a [BlockingException] of type [BlockingException.Type.PAGE] from a URL.
 *
 * @param url The URL of a web page.
 * @return a [BlockingException] of type [BlockingException.Type.PAGE] with the [source]
 * set to the host part and path of the URL with any query elements stripped.
 */
fun toPageException(url: String): BlockingException {
    val (host, path) = parse(url)
    return BlockingException("$host$path", BlockingException.Type.PAGE)
}

private fun parse(url: String): Pair<String, String> {
    var uri = Uri.parse(url.trim())
    if (uri.scheme == null) {
        // There is no http/https on the front
        uri = Uri.parse("scheme://$url")
    }
    if (uri.host.isNullOrBlank()) {
        throw SudoAdTrackerBlockerException.UrlFormatException("URL does not contain a host")
    }
    return Pair(uri.host.toString().trim(), uri.path.toString().trim().removeSuffix("/"))
}
