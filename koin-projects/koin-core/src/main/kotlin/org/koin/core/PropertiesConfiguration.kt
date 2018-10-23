package org.koin.core

import java.util.HashMap

/**
 * Properties configuration
 */
data class PropertiesConfiguration(
    val useEnvironmentProperties: Boolean = false,
    val useKoinPropertiesFile: Boolean = false,
    val extraProperties: Map<String, Any> = HashMap()
)