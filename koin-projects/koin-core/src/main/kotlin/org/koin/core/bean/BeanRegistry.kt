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
    val names = hashMapOf<String, BeanDefinition<*>>()
    val types = hashMapOf<KClass<*>, ArrayList<BeanDefinition<*>>>()

    /**
     * Add/Replace an existing bean
     *
     * @param def : Bean definition
     */
    fun declare(definition: BeanDefinition<*>) {
        val alreadyExists = checkExistingDefinition(definition)

        addDefinition(definition)

        val kw = if (alreadyExists) "override" else "declareDefinition"
        Koin.logger.info("[module] $kw $definition")
    }

    private fun checkExistingDefinition(definition: BeanDefinition<*>): Boolean {
        val alreadyExists = definitions.contains(definition)

        if (alreadyExists && !definition.options.allowOverride) {
            throw BeanOverrideException("Try to override definition with $definition, but override is not allowed. Use 'override' option in your definition or module.")
        } else {
            definition.types.forEach { type: KClass<*> ->
                deleteDefinition(type, definition)
            }
        }
        return alreadyExists
    }

    private fun addDefinition(definition: BeanDefinition<*>) {
        definitions += definition
        names[definition.name] = definition

        definition.types.forEach { type: KClass<*> ->
            val definitions: ArrayList<BeanDefinition<*>> = types.getOrElse(type) { arrayListOf() }
            definitions.add(definition)
            types[type] = definitions
        }
    }

    private fun deleteDefinition(
        type: KClass<*>,
        definition: BeanDefinition<*>
    ) {
        definitions.remove(definition)
        names.remove(definition.name)

        val definitions = types[type]
        if (definitions != null) {
            definitions.remove(definition)
            if (definitions.isEmpty()) {
                types.remove(type)
            } else {
                types[type] = definitions
            }
        }
    }

    /**
     * Search for given BeanDefinition
     * return DefinitionResolver function
     */
    fun resolveDefinitions(
        request: InstanceRequest
    ): DefinitionResolver {
        val (name, clazz) = request

        return when {
            name.isNotEmpty() -> {
                { searchByName(name) }
            }
            else -> {
                { searchByClass(clazz) }
            }
        }
    }

    private fun searchByClass(
        clazz: KClass<*>
    ): List<BeanDefinition<*>> = types[clazz] ?: emptyList()

    private fun searchByName(
        name: String
    ): List<BeanDefinition<*>> {
        val beanDefinition = names[name]
        return beanDefinition?.let { listOf(beanDefinition) } ?: emptyList()
    }

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
            getPossibleDefinitionsFromStack(definitionResolver, lastInStack)

        val candidatesForTargetScope: List<BeanDefinition<*>> = filterDefinitionsFromScope(scope, candidates)

        return checkResult(candidatesForTargetScope)
    }

    private fun filterDefinitionsFromScope(
        scope: Scope?,
        candidates: List<BeanDefinition<*>>
    ): List<BeanDefinition<*>> {
        return scope?.let {
            candidates.filter { definition -> definition.isVisibleToScope(scope) }
        } ?: candidates
    }

    private fun getPossibleDefinitionsFromStack(
        definitionResolver: () -> List<BeanDefinition<*>>,
        lastInStack: BeanDefinition<*>?
    ): List<BeanDefinition<*>> {
        val candidates =
            lastInStack?.let { definition -> definitionResolver().filter { definition.isVisibleTo(it) } }
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