package org.koin.android.viewmodel.ext.koin

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModel
import org.koin.android.viewmodel.ext.android.getViewModelByClass
import org.koin.android.viewmodel.ext.android.viewModelByClass
import org.koin.core.parameter.ParameterDefinition
import org.koin.core.parameter.emptyParameterDefinition
import org.koin.standalone.KoinComponent

data class ViewModelParameters(
    val key: String? = null,
    val name: String? = null,
    val parameters: ParameterDefinition = emptyParameterDefinition()
)

/**
 *
 */
inline fun <reified T : ViewModel> KoinComponent.viewModel(
    lifecycleOwner: LifecycleOwner,
    parameters: ViewModelParameters = ViewModelParameters()
) = lifecycleOwner.viewModelByClass(
    T::class,
    parameters.key,
    parameters.name,
    null,
    parameters.parameters
)

/**
 * 
 */
inline fun <reified T : ViewModel> KoinComponent.getViewModel(
    lifecycleOwner: LifecycleOwner,
    parameters: ViewModelParameters = ViewModelParameters()
) = lifecycleOwner.getViewModelByClass(
    T::class, parameters.key,
    parameters.name,
    null,
    parameters.parameters
)