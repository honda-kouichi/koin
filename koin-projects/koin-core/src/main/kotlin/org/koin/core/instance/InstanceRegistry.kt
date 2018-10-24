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
package org.koin.core.instance

import org.koin.core.Koin
import org.koin.core.bean.BeanRegistry
import org.koin.core.bean.DefinitionResolver
import org.koin.core.parameter.ParameterDefinition
import org.koin.core.parameter.emptyParameterDefinition
import org.koin.core.path.PathRegistry
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeRegistry
import org.koin.core.scope.getScope
import org.koin.core.stack.ResolutionStack
import org.koin.core.time.logDuration
import org.koin.dsl.definition.BeanDefinition
import org.koin.error.KoinResolutionException
import org.koin.ext.fullname
import kotlin.reflect.KClass

/**
 * Instance Resolver
 *
 * @author Arnaud Giuliani
 */
class InstanceRegistry(
    val beanRegistry: BeanRegistry,
    val instanceFactory: InstanceFactory,
    val pathRegistry: PathRegistry,
    private val scopeRegistry: ScopeRegistry
) {

    private val resolutionStack = ResolutionStack()

    /**
     * execute instance from InstanceRequest
     */
    fun <T : Any> resolve(request: InstanceRequest): T {

        val definitionResolver: DefinitionResolver = beanRegistry
            .resolveDefinitions(request)

        val (_, clazz, scope, parameters) = request
        return proceedResolution(clazz, scope, parameters, definitionResolver)
    }

    /**
     * Resolve a dependency for its bean definition
     * @param scope - associated scope
     * @param clazz - Class
     * @param parameters - Parameters
     * @param definitionResolver - function to find bean definitions
     */
    private fun <T : Any> proceedResolution(
        clazz: KClass<*>,
        scope: Scope?,
        parameters: ParameterDefinition,
        definitionResolver: DefinitionResolver
    ): T = synchronized(this) {

        var resultInstance: T? = null
        val clazzName = clazz.fullname()
        val logIndent: String = resolutionStack.indent()

        logDuration("$logIndent!-- [$clazzName] resolved") {
            try {
                val startCharacter = if (resolutionStack.isEmpty()) "+" else "+"
                Koin.logger.info("$logIndent$startCharacter-- '$clazzName'")

                val beanDefinition: BeanDefinition<T> =
                    getBeanDefinition(logIndent, clazzName, definitionResolver, scope)

                // Retrieve scope from DSL
                val definitionScopeId = beanDefinition.getScope()
                val targetScope: Scope? = scope ?: scopeRegistry.getScope(definitionScopeId)

                resolutionStack.execute(beanDefinition) {
                    val (instance, created) = resolveInstance(
                        logIndent,
                        clazzName,
                        beanDefinition,
                        parameters,
                        targetScope
                    )

                    // Log creation
                    if (created) {
                        Koin.logger.info("$logIndent\\-- (*) Created")
                    }
                    resultInstance = instance
                }
            } catch (executionError: Exception) {
                Koin.logger.err("Error while resolving instance type '$clazzName' - due to error: $executionError ")
                resolutionStack.clear()
                throw KoinResolutionException(executionError)
            }
        }

        return if (resultInstance != null) resultInstance!! else error("Could not createInstanceHolder instance for $clazzName")
    }

    private fun <T : Any> resolveInstance(
        logIndent: String,
        clazzName: String,
        beanDefinition: BeanDefinition<T>,
        parameters: ParameterDefinition,
        targetScope: Scope?
    ): Instance<T> {
        return logDuration("$logIndent!-- [$clazzName] instance") {
            instanceFactory.getInstance(
                beanDefinition,
                parameters,
                targetScope
            )
        }
    }

    private fun <T : Any> getBeanDefinition(
        logIndent: String,
        clazzName: String,
        definitionResolver: DefinitionResolver,
        scope: Scope?
    ): BeanDefinition<T> {
        return logDuration("$logIndent!-- [$clazzName] definition") {
            beanRegistry.retrieveDefinition(
                definitionResolver,
                resolutionStack.last(),
                scope
            )
        }
    }

    /**
     * Create instances at start - tagged eager
     * @param defaultParameters
     */
    fun createEagerInstances() {
        val definitions = beanRegistry.definitions.filter { it.options.isEager }

        if (definitions.isNotEmpty()) {
            Koin.logger.info("Creating instances ...")
            createEagerInstancesForDefinitions(definitions)
        }
    }

    private fun createEagerInstancesForDefinitions(
        definitions: Collection<BeanDefinition<*>>
    ) {
        definitions.forEach { def ->
            proceedResolution(
                def.primaryType,
                null,
                emptyParameterDefinition()
            ) { listOf(def) }
        }
    }

    /**
     * Dry Run - run each definition
     */
    fun dryRun() {
        createEagerInstancesForDefinitions(beanRegistry.definitions)
    }

    /**
     * Close res
     */
    fun close() {
        Koin.logger.debug("[Close] Closing instance resolver")
        resolutionStack.clear()
        instanceFactory.clear()
        beanRegistry.clear()
    }
}

//typealias DefinitionFilter = (BeanDefinition<*>) -> Boolean