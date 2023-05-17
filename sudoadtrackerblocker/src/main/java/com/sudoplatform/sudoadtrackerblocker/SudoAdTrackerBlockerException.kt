/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudoadtrackerblocker

/**
 * Defines the exceptions thrown by the methods of the [SudoAdTrackerBlockerClient].
 *
 * @property message [String] Accompanying message for the exception.
 * @property cause [Throwable] The cause for the exception.
 */
sealed class SudoAdTrackerBlockerException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause) {
    /** A configuration item that is needed is missing */
    class ConfigurationException(message: String? = null, cause: Throwable? = null) :
        SudoAdTrackerBlockerException(message = message, cause = cause)
    class FilterEngineNotReadyException(message: String? = null, cause: Throwable? = null) :
        SudoAdTrackerBlockerException(message = message, cause = cause)
    class DataFormatException(message: String? = null, cause: Throwable? = null) :
        SudoAdTrackerBlockerException(message = message, cause = cause)
    class UrlFormatException(message: String? = null, cause: Throwable? = null) :
        SudoAdTrackerBlockerException(message = message, cause = cause)
    class UnauthorizedUserException(message: String? = null, cause: Throwable? = null) :
        SudoAdTrackerBlockerException(message = message, cause = cause)
    class NoSuchRulesetException(message: String? = null, cause: Throwable? = null) :
        SudoAdTrackerBlockerException(message = message, cause = cause)
    class FailedException(message: String? = null, cause: Throwable? = null) :
        SudoAdTrackerBlockerException(message = message, cause = cause)
    class UnknownException(cause: Throwable) :
        SudoAdTrackerBlockerException(cause = cause)
}
