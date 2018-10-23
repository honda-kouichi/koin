package org.koin.error

/**
 * Error while resolving instance
 */
class KoinResolutionException(e: Exception) : Exception(e)