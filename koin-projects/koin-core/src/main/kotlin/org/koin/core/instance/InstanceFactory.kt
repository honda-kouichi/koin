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
import org.koin.core.parameter.ParameterDefinition
import org.koin.core.scope.Scope
import org.koin.dsl.definition.BeanDefinition
import org.koin.dsl.definition.Kind
import org.koin.dsl.path.Path
import org.koin.error.ClosedScopeException
import org.koin.error.NoScopeException

/**
 * Instance factory - handle objects creation against BeanRegistry
 * @author - Arnaud GIULIANI
 */
open class InstanceFactory {

    val instances = hashMapOf<String, InstanceHolder<*>>()
    val callbacks = ArrayList<ModuleCallBack>()

    /**
     * Retrieve or createInstanceHolder instance from bean definition
     * @return Instance / has been created
     */
    fun <T : Any> getInstance(
        def: BeanDefinition<T>,
        p: ParameterDefinition,
        scope: Scope? = null
    ): Instance<T> {
        // find holder
        val holder: InstanceHolder<T> = getOrCreateInstanceHolder(def, scope)

        if (holder is ScopeInstanceHolder && holder.scope.isClosed) {
            throw ClosedScopeException("Can't reuse a closed scope : $scope")
        }

        return resolveInstance(holder, p)
    }

    private fun <T : Any> resolveInstance(
        holder: InstanceHolder<T>,
        p: ParameterDefinition
    ): Instance<T> = holder.get(p)

    private fun <T : Any> getOrCreateInstanceHolder(
        def: BeanDefinition<T>,
        scope: Scope?
    ): InstanceHolder<T> {
        var holder: InstanceHolder<T>? = find(def)
        if (holder == null) {
            holder = createInstanceHolder(def, scope)
            saveHolder(def, holder)
        }
        return holder
    }

    private fun <T : Any> saveHolder(
        def: BeanDefinition<T>,
        holder: InstanceHolder<T>
    ) {
        instances[def.name] = holder
    }

    /**
     * Find actual InstanceHolder
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> find(def: BeanDefinition<T>): InstanceHolder<T>? =
        instances[def.name] as? InstanceHolder<T>

    /**
     * Create InstanceHolder
     */
    open fun <T : Any> createInstanceHolder(def: BeanDefinition<T>, scope: Scope? = null): InstanceHolder<T> {
        return when (def.kind) {
            Kind.Single -> SingleInstanceHolder(def)
            Kind.Factory -> FactoryInstanceHolder(def)
            Kind.Scope -> {
                if (scope != null && !scope.isClosed) {
                    scope.instanceFactory = this
                    ScopeInstanceHolder(
                        def,
                        scope
                    )
                } else {
                    if (scope == null) throw NoScopeException("Definition '$def' has to be used with a scope. Please createInstanceHolder and specify a scope to use with your definition")
                    else throw ClosedScopeException("Can't reuse a closed scope : $scope")
                }
            }
        }
    }

    /**
     * Release definition instance
     */
    fun releaseInstance(definition: BeanDefinition<*>) {
        if (definition.kind == Kind.Scope) {
            Koin.logger.debug("releaseInstance $definition")
            val holder = find(definition)
            holder?.let {
                instances.remove(definition.name)
            }
        }
    }

    /**
     * Delete Instance Holder
     */
    fun deleteInstance(definition: BeanDefinition<*>) {
        instances.remove(definition.name)
    }

    /**
     * Clear all resources
     */
    fun clear() {
        instances.clear()
    }

    /**
     * Release single instance from their Path
     */
    @Deprecated("Release path should not be use anymore. Use Scope API instead")
    fun releasePath(path: Path) {
        val singeInstanceNames =
            instances.values.filter { it.bean.kind == Kind.Single && path.isVisible(it.bean.path) }
                .map { it.bean.name }

        singeInstanceNames.forEach {
            instances.remove(it)
        }

        callbacks.forEach { it.onRelease(path.name) }
    }

    /**
     * Register ModuleCallBack
     */
    @Deprecated("Uset he Scope API.")
    fun register(callback: ModuleCallBack) {
        callbacks += callback
    }
}