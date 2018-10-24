package org.koin.core.instance

/**
 * Handle Path releaseInstance
 * Deprecated - Use Scope APi
 */
@Deprecated("Please use Scope API instead.")
interface ModuleCallBack {

    fun onRelease(path: String)
}