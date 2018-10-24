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
package org.koin.androidx.scope.ext.android

import android.content.ComponentCallbacks
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.koin.android.ext.android.getKoin
import org.koin.androidx.scope.ScopeObserver
import org.koin.core.KoinContext
import org.koin.core.scope.Scope
import org.koin.standalone.StandAloneContext


/**
 * LifecycleOwner extensions
 *
 * @author Arnaud Giuliani
 */

/**
 * Set a Scope Observer onto the actual LifecycleOwner component
 * will close the bound scopes on lifecycle event
 * @see ScopeObserver
 * @param event : lifecycle event - default ON_DESTROY
 * @param scopes
 */
fun LifecycleOwner.bindScope(scope: Scope, event: Lifecycle.Event = Lifecycle.Event.ON_DESTROY) {
    lifecycle.addObserver(ScopeObserver(event, this, scope))
}

private fun LifecycleOwner.getKoin() = (this as ComponentCallbacks).getKoin()

/**
 * Get or createInstanceHolder Scope
 * @param scope Id
 */
fun LifecycleOwner.getOrCreateScope(id : String) : Scope {
    return getKoin().getOrCreateScope(id)
}

/**
 * Get Scope
 * @param scope Id
 */
fun LifecycleOwner.getScope(id : String) : Scope {
    return getKoin().getScope(id)
}

/**
 * Create Scope
 * @param scope Id
 */
fun LifecycleOwner.createScope(id : String) : Scope {
    return getKoin().createScope(id)
}

