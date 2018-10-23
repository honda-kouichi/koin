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

package org.koin.android.ext.koin

import android.app.Application
import android.content.Context
import org.koin.core.Koin
import org.koin.core.KoinConfiguration
import org.koin.dsl.definition.BeanDefinition
import java.util.*

/**
 * Koin extensions for Android
 *
 * @author Arnaud Giuliani
 */

/**
 * Add Context instance to Koin container
 * @param androidContext - Context
 */
infix fun KoinConfiguration.with(androidContext: Context): KoinConfiguration {
    Koin.logger.info("[init] declareDefinition Android Context")
    declareAndroidContext(androidContext)
    if (androidContext is Application) {
        declareAndroidApplication(androidContext)
    }
    return this
}

private fun KoinConfiguration.declareAndroidApplication(androidContext: Application) {
    getKoin().declareDefinition(
        BeanDefinition.create(
            name = "android.app.Application",
            definition = { androidContext }
        )
    )
}

private fun KoinConfiguration.declareAndroidContext(androidContext: Context) {
    getKoin().declareDefinition(
        BeanDefinition.create(
            name = "android.content.Context",
            definition = { androidContext }
        )
    )
}

/**
 * Load properties file from Assets
 * @param androidContext
 * @param koinPropertyFile
 */
fun KoinConfiguration.loadPropertiesForAndroid(
    androidContext: Context,
    koinPropertyFile: String = "koin.properties"
): KoinConfiguration {
    val koinProperties = Properties()
    try {
        androidContext.assets.open(koinPropertyFile).use { koinProperties.load(it) }
        val nb = getKoin().loadProperties(koinProperties)
        Koin.logger.info("[Android-Properties] loaded $nb properties from assets/koin.properties")
    } catch (e: Exception) {
        Koin.logger.info("[Android-Properties] no properties in assets/$koinPropertyFile to load")
    }
    return this
}
