package org.koin.core.instance

import org.koin.core.parameter.ParameterDefinition
import org.koin.dsl.definition.BeanDefinition
import org.koin.error.BeanInstanceCreationException

interface InstanceHolder<T> {

    val bean: BeanDefinition<T>

    fun <T> get(parameters: ParameterDefinition): Instance<T>

    @Suppress("UNCHECKED_CAST")
    fun <T> create(parameters: ParameterDefinition): T {
        try {
            val parameterList = parameters()
            return bean.definition(parameterList) as T
        } catch (e: Throwable) {
            val stack = e.stackTrace.takeWhile { !it.className.contains("sun.reflect") }
                .joinToString("\n\t\t")
            throw BeanInstanceCreationException("Can't create definition for '$bean' due to error :\n\t\t${e.message}\n\t\t$stack")
        }
    }

    fun release()
}

data class Instance<T>(val value: T, val created: Boolean)