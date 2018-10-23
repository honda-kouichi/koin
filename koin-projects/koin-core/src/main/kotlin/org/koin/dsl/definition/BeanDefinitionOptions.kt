package org.koin.dsl.definition


/**
 * Bean Definition Attributes
 */
typealias BeanDefinitionAttributes = HashMap<String, Any>

/**
 * Additional options & attributes for Bean Definition
 * @param isEager
 * @param allowOverride
 * @param attributes
 */
data class BeanDefinitionOptions(
    val isEager: Boolean = false,
    val allowOverride: Boolean = false
)
