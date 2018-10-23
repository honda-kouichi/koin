/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.koin.core.bean

import org.koin.core.Koin
import org.koin.core.instance.DefinitionFilter
import org.koin.core.instance.InstanceRequest
import org.koin.core.scope.Scope
import org.koin.core.scope.isVisibleToScope
import org.koin.dsl.definition.BeanDefinition
import org.koin.error.BeanOverrideException
import org.koin.error.DependencyResolutionException
import org.koin.error.NoBeanDefFoundException
import kotlin.reflect.KClass


/**
 * Bean registry
 * gather definitions of beans & communicate with instance factory to handle instances
 *
 * @author - Arnaud GIULIANI
 */
class BeanRegistry() {

    val definitions = hashSetOf<BeanDefinition<*>>()

    /**
     * Add/Replace an existing bean
     *
     * @param def : Bean definition
     */
    fun declare(definition: BeanDefinition<*>) {
        val alreadyExists = definitions.contains(definition)

        if (alreadyExists && !definition.options.allowOverride) {
            throw BeanOverrideException("Try to override definition with $definition, but override is not allowed. Use 'override' option in your definition or module.")
        } else {
            definitions.remove(definition)
        }

        definitions += definition

        val kw = if (alreadyExists) "override" else "declareDefinition"
        Koin.logger.info("[module] $kw $definition")
    }

    /**
     * Search for given BeanDefinition
     * return DefinitionResolver function
     */
    fun resolveDefinitions(
        request: InstanceRequest,
        filterFunction: DefinitionFilter?
    ): DefinitionResolver {
        val (name, clazz) = request

        val definitions = filterFunction?.let { definitions.filter(it) } ?: definitions

        return when {
            name.isNotEmpty() -> {
                { searchByNameAndClass(definitions, name, clazz) }
            }
            else -> {
                { searchByClass(definitions, clazz) }
            }
        }
    }

    private fun searchByClass(
        definitions: Collection<BeanDefinition<*>>,
        clazz: KClass<*>
    ): List<BeanDefinition<*>> = definitions.filter { clazz in it.types }

    private fun searchByNameAndClass(
        definitions: Collection<BeanDefinition<*>>,
        name: String,
        clazz: KClass<*>
    ): List<BeanDefinition<*>> = definitions.filter { name == it.name && clazz in it.types }


    /**
     * Retrieve bean definition
     * @param clazzName - class canonicalName
     * @param modulePath - Module path
     * @param definitionResolver - function to find bean definition
     * @param lastInStack - to check visibility with last bean in stack
     */
    fun <T> retrieveDefinition(
        definitionResolver: () -> List<BeanDefinition<*>>,
        lastInStack: BeanDefinition<*>?,
        scope: Scope?
    ): BeanDefinition<T> {
        val candidates: List<BeanDefinition<*>> =
            getPossibleDefinitions(definitionResolver, lastInStack)

        val candidatesForTargetScope = scope?.let {
            candidates.filter { definition -> definition.isVisibleToScope(scope) }
        } ?: candidates

        return checkResult(candidatesForTargetScope)
    }

    private fun getPossibleDefinitions(
        definitionResolver: () -> List<BeanDefinition<*>>,
        lastInStack: BeanDefinition<*>?
    ): List<BeanDefinition<*>> {
        val candidates =
            lastInStack?.let { definitionResolver().filter { lastInStack.isVisibleTo(it) } }
                ?: definitionResolver()
        return candidates.distinct()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> checkResult(
        definitions: List<BeanDefinition<*>>
    ): BeanDefinition<T> {
        return when (definitions.size) {
            1 -> definitions.first() as BeanDefinition<T>
            0 -> throw NoBeanDefFoundException("No compatible definition found. Check your module definition")
            else -> throw DependencyResolutionException(
                "Multiple definitions found :\n\t${definitions.joinToString(
                    "\n\t"
                )}\n\tCheck your modules definition, use inner modules visibility or definition names."
            )
        }
    }

    /**
     * Clear resources
     */
    fun clear() {
        definitions.clear()
    }
}

typealias DefinitionResolver = () -> List<BeanDefinition<*>>