package org.koin.ext

import kotlin.reflect.KClass


internal fun <T : Any> KClass<T>.fullname(): String = java.canonicalName

internal fun <T : Any> KClass<T>.simpleName(): String = java.simpleName

