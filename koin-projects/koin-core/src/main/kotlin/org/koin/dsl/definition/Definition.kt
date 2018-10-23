package org.koin.dsl.definition

import org.koin.core.parameter.ParameterList

/**
 * Type Definition function - what's build a given component T
 */
typealias Definition<T> = (ParameterList) -> T