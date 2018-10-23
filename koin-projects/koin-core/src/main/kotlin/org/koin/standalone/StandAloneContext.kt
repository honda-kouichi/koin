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
package org.koin.standalone

import org.koin.core.Koin
import org.koin.core.KoinConfiguration
import org.koin.core.PropertiesConfiguration
import org.koin.core.time.measureDuration
import org.koin.dsl.module.Module
import org.koin.error.AlreadyStartedException
import org.koin.log.Logger
import org.koin.log.PrintLogger

/**
 * Koin agnostic context support
 * @author - Arnaud GIULIANI
 */
object StandAloneContext {

    private var isStarted = false

    /**
     * Koin Builder Configuration
     */
    var koinConfiguration: KoinConfiguration? = null

    /**
     * Retrieve current KoinConfiguration
     */
    fun getKoinConfig(): KoinConfiguration = koinConfiguration ?: error("try to use koinConfiguration but is null")

    /**
     * Return current Koin context or create it
     */
    fun getCurrentContext(): KoinConfiguration {
        if (koinConfiguration == null) {
            Koin.logger.info("[context] create")
            koinConfiguration = KoinConfiguration.create()
        }
        return getKoinConfig()
    }

    /**
     * Load Koin modules - whether Koin is already started or not
     * allow late module definition load (e.g: libraries ...)
     *
     * @param modules : Module
     */
    fun loadKoinModules(vararg modules: Module): KoinConfiguration {
        return loadKoinModules(modules.toList())
    }

    /**
     * Load Koin modules - whether Koin is already started or not
     * allow late module definition load (e.g: libraries ...)
     *
     * @param modules : List of Module
     */
    fun loadKoinModules(modules: List<Module>): KoinConfiguration {
        return getCurrentContext().loadModules(modules)
    }

    /**
     * Koin starter function to load modules and extraProperties
     * Throw AlreadyStartedException if already started
     * @param list : Modules
     * @param useEnvironmentProperties - use environment extraProperties
     * @param useKoinPropertiesFile - use /koin.extraProperties file
     * @param extraProperties - extra extraProperties
     * @param logger - Koin logger
     */
    fun startKoin(
        list: List<Module>,
        propertiesConfiguration: PropertiesConfiguration = PropertiesConfiguration(),
        logger: Logger = PrintLogger()
    ): KoinConfiguration {
        if (isStarted) {
            throw AlreadyStartedException("Koin is already started. Run startKoin only once or use loadKoinModules")
        }
        val (koin, duration) = startNewContext(logger, propertiesConfiguration, list)
        isStarted = true
        Koin.logger.debug("Koin started in $duration ms")
        return koin
    }

    private fun startNewContext(
        logger: Logger,
        propertiesConfiguration: PropertiesConfiguration,
        list: List<Module>
    ): Pair<KoinConfiguration, Double> {
        val koin = getCurrentContext()
        val duration = measureDuration {
            Koin.logger = logger
            koin.apply {
                loadAllProperties(propertiesConfiguration)
                loadModules(list)
            }
        }
        return Pair(koin, duration)
    }

    /**
     * Close actual Koin context
     * - drop akk instances & definitions
     */
    fun stopKoin() = synchronized(this) {
        // Close all
        koinConfiguration?.close()
        koinConfiguration = null
        isStarted = false
    }

    /**
     * Close actual Koin context
     */
    @Deprecated("Renamed, please use stopKoin() instead.")
    fun closeKoin() = stopKoin()
}