package org.koin.test.core.instance

import org.koin.core.instance.InstanceFactory
import org.koin.core.instance.InstanceHolder
import org.koin.core.scope.Scope
import org.koin.dsl.definition.BeanDefinition

/**
 * Sandbox Instance Factory
 * Create mock version of each dependency to help execute all dependency graph without executing the real instance
 */
class SandboxInstanceFactory() : InstanceFactory() {

    override fun <T: Any> createInstanceHolder(def: BeanDefinition<T>, scope: Scope?): InstanceHolder<T> = SandboxInstanceHolder(def)

}