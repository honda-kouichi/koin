package org.koin.core

import org.koin.core.instance.ModuleCallBack
import org.koin.core.scope.ScopeCallback
import org.koin.core.time.measureDuration
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.path.Path
import java.util.*

/**
 * KoinConfiguration
 *
 * API to Help build & use KoinContext
 */
class KoinConfiguration(private val koinContext: KoinContext) {

    private val propertyResolver = koinContext.propertyResolver
    private val instanceRegistry = koinContext.instanceRegistry
    private val beanRegistry = koinContext.instanceRegistry.beanRegistry
    private val pathRegistry = koinContext.instanceRegistry.pathRegistry
    private val instanceFactory = koinContext.instanceRegistry.instanceFactory
    private val scopeRegistry = koinContext.scopeRegistry

    /**
     * load given list of module instances into current StandAlone koin context
     */
    fun loadModules(modules: Collection<Module>): KoinConfiguration = synchronized(this) {
        val duration = measureDuration {
            modules.forEach { module ->
                registerDefinitions(module(koinContext))
            }

            Koin.logger.info("[modules] loaded ${beanRegistry.definitions.size} definitions")
        }
        Koin.logger.debug("[modules] loaded in $duration ms")

        instanceRegistry.createEagerInstances()
        return this
    }

    /**
     * Register moduleDefinition definitions & subModules
     */
    //TODO Refactor
    private fun registerDefinitions(
        moduleDefinition: ModuleDefinition,
        parentModuleDefinition: ModuleDefinition? = null,
        path: Path = Path.root()
    ) {
        val modulePath: Path =
            pathRegistry.makePath(moduleDefinition.path, parentModuleDefinition?.path)
        val consolidatedPath =
            if (path != Path.root()) modulePath.copy(parent = path) else modulePath

        pathRegistry.savePath(consolidatedPath)

        // Add definitions & propagate eager/override
        moduleDefinition.definitions.forEach { definition ->
            val eager =
                if (moduleDefinition.createOnStart) moduleDefinition.createOnStart else definition.isEager
            val override =
                if (moduleDefinition.override) moduleDefinition.override else definition.allowOverride
            val name = if (definition.name.isEmpty()) {
                val pathString =
                    if (consolidatedPath == Path.Companion.root()) "" else "$consolidatedPath."
                "$pathString${definition.clazz.name()}"
            } else definition.name
            val def = definition.copy(
                name = name,
                isEager = eager,
                allowOverride = override,
                path = consolidatedPath
            )
            instanceFactory.delete(def)
            beanRegistry.declare(def)
        }

        // Check sub contexts
        moduleDefinition.subModules.forEach { subModule ->
            registerDefinitions(
                subModule,
                moduleDefinition,
                consolidatedPath
            )
        }
    }

    /**
     * Load Koin properties - whether Koin is already started or not
     * Will look at koin.properties file
     *
     * @param useEnvironmentProperties - environment properties
     * @param useKoinPropertiesFile - koin.properties file
     * @param extraProperties - additional properties
     */
    fun loadAllProperties(
        props: PropertiesConfiguration
    ): KoinConfiguration = synchronized(this) {
        if (props.useKoinPropertiesFile) {
            Koin.logger.info("[properties] load koin.properties")
            loadPropertiesFromFile()
        }

        if (props.extraProperties.isNotEmpty()) {
            Koin.logger.info("[properties] load extras properties : ${props.extraProperties.size}")
            loadProperties(props.extraProperties)
        }

        if (props.useEnvironmentProperties) {
            Koin.logger.info("[properties] load environment properties")
            loadEnvironmentProperties()
        }
        return this
    }

    /**
     * Inject properties to context
     */
    private fun loadProperties(props: Map<String, Any>) {
        if (props.isNotEmpty()) {
            propertyResolver.addAll(props)
        }
    }

    /**
     * Inject all properties from koin properties file to context
     */
    private fun loadPropertiesFromFile(koinFile: String = "/koin.properties") {
        val content = Koin::class.java.getResource(koinFile)?.readText()
        content?.let {
            val koinProperties = Properties()
            koinProperties.load(content.byteInputStream())
            val nb = propertyResolver.import(koinProperties)
            Koin.logger.info("[properties] loaded $nb properties from '$koinFile' file")
        }
    }

    /**
     * Inject all system properties to context
     */
    private fun loadEnvironmentProperties() {
        val n1 = propertyResolver.import(System.getProperties())
        Koin.logger.info("[properties] loaded $n1 properties from properties")
        val n2 = propertyResolver.import(System.getenv().toProperties())
        Koin.logger.info("[properties] loaded $n2 properties from env properties")
    }

    /**
     * Register ScopeCallback - being notified on Scope closing
     * @see ScopeCallback - ScopeCallback
     */
    fun registerScopeCallback(callback: ScopeCallback) {
        scopeRegistry.register(callback)
    }

    /**
     * Register ModuleCallBack - being notified on Path release
     * @see ScopeCallback - ModuleCallBack
     *
     * Deprecared - Use the Scope API
     */
    @Deprecated("Please use the Scope API instead.")
    fun registerModuleCallBack(callback: ModuleCallBack) {
        instanceRegistry.instanceFactory.register(callback)
    }

    /**
     * Get Koin context
     */
    fun getKoin(): KoinContext = koinContext

    /**
     * Close actual context
     */
    fun close() {
        koinContext.close()
    }

    companion object {
        /**
         * Create Koin context builder
         */
        fun create(): KoinConfiguration = KoinConfiguration(KoinContext.create())
    }
}

