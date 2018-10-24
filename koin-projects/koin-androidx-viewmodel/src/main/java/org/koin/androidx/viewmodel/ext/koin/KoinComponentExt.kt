package org.koin.androidx.viewmodel.ext.koin

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import org.koin.androidx.viewmodel.ext.android.ViewModelStoreOwnerDefinition
import org.koin.androidx.viewmodel.ext.android.getViewModelByClass
import org.koin.androidx.viewmodel.ext.android.viewModelByClass
import org.koin.core.parameter.ParameterDefinition
import org.koin.core.parameter.emptyParameterDefinition
import org.koin.standalone.KoinComponent

/**
 * ViewMOdel request parameters
 */
data class ViewModelParameters(
    val key: String? = null,
    val name: String? = null,
    val from: ViewModelStoreOwnerDefinition? = null,
    val parameters: ParameterDefinition = emptyParameterDefinition()
)

/**
 * Lazy Get ViewModel for given lifecycleOwner component
 */
inline fun <reified T : ViewModel> KoinComponent.viewModel(
    lifecycleOwner: LifecycleOwner,
    parameters: ViewModelParameters = ViewModelParameters()
) = lifecycleOwner.viewModelByClass(
    T::class,
    parameters.key,
    parameters.name,
    parameters.from,
    parameters.parameters
)

/**
 * Get ViewModel for given lifecycleOwner component
 */
inline fun <reified T : ViewModel> KoinComponent.getViewModel(
    lifecycleOwner: LifecycleOwner,
    parameters: ViewModelParameters = ViewModelParameters()
) = lifecycleOwner.getViewModelByClass(
    T::class, parameters.key,
    parameters.name,
    parameters.from,
    parameters.parameters
)