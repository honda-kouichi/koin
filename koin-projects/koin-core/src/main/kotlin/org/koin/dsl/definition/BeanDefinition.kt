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
package org.koin.dsl.definition

import org.koin.core.scope.getScope
import org.koin.dsl.path.Path
import org.koin.error.DefinitionBindingException
import kotlin.reflect.KClass


/**
 * Bean definition
 * @author - Arnaud GIULIANI
 *
 * Gather type of T
 * defined by lazy/function
 * has a type (primaryType)
 * has a BeanType : default singleton
 * has a canonicalName, if specified
 *
 * @param name - bean canonicalName
 * @param types - all biund types
 * @param definition - bean definition function
 * @param primaryType - bean class
 * @param path - logical path
 * @param kind - bean definition Kind
 * @param attributes
 * @param options
 */
data class BeanDefinition<T>(
    val name: String = "",
    var types: List<KClass<*>> = arrayListOf(),
    inline val definition: Definition<T>,
    val primaryType: KClass<*>,
    val path: Path = Path.root(),
    val kind: Kind = Kind.Single,
    val attributes: BeanDefinitionAttributes = BeanDefinitionAttributes(),
    val options: BeanDefinitionOptions = BeanDefinitionOptions()
) {

    /**
     * Add a compatible type to current bounded definition
     */
    infix fun bind(clazz: KClass<*>): BeanDefinition<*> {
        if (!clazz.java.isAssignableFrom(this.primaryType.java)) {
            throw DefinitionBindingException("Can't bind type '$clazz' for definition $this")
        } else {
            types += clazz
        }
        return this
    }

    /**
     * Check visibility if "this" can see "other"
     */
    fun isVisibleTo(other: BeanDefinition<*>) = other.path.isVisible(this.path)

    override fun toString(): String {
        val beanName = if (name.isEmpty()) "" else "name='$name',"
        val clazz = "class='${primaryType.java.canonicalName}'"
        val type = "$kind"
        val path = if (path != Path.root()) ", path:'$path'" else ""
        return "$type [$beanName$clazz$path]"
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BeanDefinition<*>

        if (name != other.name) return false
        if (primaryType != other.primaryType) return false
        if (attributes != other.attributes) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode() + primaryType.hashCode() + attributes.hashCode()
    }

    fun getScopeName(): String {
        val scope = getScope()
        return if (scope.isEmpty()) "" else "$scope."
    }

    companion object {

        /**
         * Create a definition
         */
        inline fun <reified T> create(
            name: String = "",
            kind: Kind = Kind.Single,
            noinline definition: Definition<T>,
            options: BeanDefinitionOptions = BeanDefinitionOptions()
        ): BeanDefinition<T> {
            val clazz = T::class
            return BeanDefinition(
                name = name,
                primaryType = clazz,
                types = listOf(clazz),
                definition = definition,
                kind = kind,
                options = options
            )
        }
    }
}