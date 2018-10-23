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
import org.koin.core.name
import org.koin.core.scope.Scope
import org.koin.core.scope.isVisibleToScope
import org.koin.dsl.definition.BeanDefinition
import org.koin.error.BeanOverrideException
import org.koin.error.DependencyResolutionException
import org.koin.error.NoBeanDefFoundException
import org.koin.error.NotVisibleException
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

        return  when {
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
    @Suppress("UNCHECKED_CAST")
    fun <T> retrieveDefinition(
        clazz: KClass<*>,
        scope: Scope?,
        definitionResolver: () -> List<BeanDefinition<*>>,
        lastInStack: BeanDefinition<*>?
    ): BeanDefinition<T> {
        val candidates: List<BeanDefinition<*>> = (if (lastInStack != null) {
            val found = definitionResolver()
            val filteredByVisibility = found.filter { lastInStack.isVisibleTo(it) }
            if (found.isNotEmpty() && filteredByVisibility.isEmpty()) {
                throw NotVisibleException("Can't proceedResolution '$clazz' - Definition is not visible from last definition : $lastInStack")
            }
            filteredByVisibility
        } else {
            definitionResolver()
        }).distinct()

        val filterByScope = if (scope != null) {
            candidates.filter { it.isVisibleToScope(scope) }
        } else candidates

        return when {
            filterByScope.size == 1 -> filterByScope.first() as BeanDefinition<T>
            filterByScope.isEmpty() -> throw NoBeanDefFoundException("No compatible definition found for type '${clazz.name()}'. Check your module definition")
            else -> throw DependencyResolutionException(
                "Multiple definitions found for type '$clazz' - Koin can't choose between :\n\t${filterByScope.joinToString(
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