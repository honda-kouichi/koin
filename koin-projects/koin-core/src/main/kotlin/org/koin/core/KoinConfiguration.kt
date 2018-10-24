package org.koin.core

import org.koin.core.instance.ModuleCallBack
import org.koin.core.scope.ScopeCallback
import org.koin.core.time.logDuration
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.definition.BeanDefinition
import org.koin.dsl.definition.BeanDefinitionOptions
import org.koin.dsl.module.Module
import org.koin.dsl.path.Path
import org.koin.ext.simpleName
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
        logDuration("[modules] loaded") {
            modules.forEach { module ->
                registerDefinitions(module(koinContext))
            }

            Koin.logger?.info("[modules] loaded ${beanRegistry.definitions.size} definitions")
        }

        return this
    }

    fun createEagerInstances() {
        instanceRegistry.createEagerInstances()
    }

    /**
     * Register moduleDefinition definitions & subModules
     */
    private fun registerDefinitions(
        moduleDefinition: ModuleDefinition,
        parentModuleDefinition: ModuleDefinition? = null,
        path: Path = Path.root()
    ) {
        val modulePath =
            savePath(moduleDefinition, parentModuleDefinition, path)

        // Add definitions & propagate eager/override
        moduleDefinition.definitions.forEach { definition ->
            val def = prepareDefinitionOptions(definition, moduleDefinition, modulePath)
            saveDefinition(def)
        }

        // Check sub contexts
        moduleDefinition.subModules.forEach { subModule ->
            proceedNextModule(subModule, moduleDefinition, modulePath)
        }
    }

    private fun proceedNextModule(
        subModule: ModuleDefinition,
        moduleDefinition: ModuleDefinition,
        modulePath: Path
    ) {
        registerDefinitions(
            subModule,
            moduleDefinition,
            modulePath
        )
    }

    private fun saveDefinition(def: BeanDefinition<out Any?>) {
        instanceFactory.deleteInstance(def)
        beanRegistry.declare(def)
    }

    private fun prepareDefinitionOptions(
        definition: BeanDefinition<*>,
        moduleDefinition: ModuleDefinition,
        modulePath: Path
    ): BeanDefinition<out Any?> {
        val definitionOptions = definition.options
        val eager =
            if (moduleDefinition.createOnStart) moduleDefinition.createOnStart else definitionOptions.isEager
        val override =
            if (moduleDefinition.override) moduleDefinition.override else {
                definitionOptions.allowOverride
            }
        val name = defineDefinitionName(definition, modulePath)
        return updateDefinition(definition, name, eager, override, modulePath)
    }

    private fun savePath(
        moduleDefinition: ModuleDefinition,
        parentModuleDefinition: ModuleDefinition?,
        path: Path
    ): Path {
        val modulePath: Path =
            makePath(moduleDefinition, parentModuleDefinition)
        val consolidatedPath =
            getConsolidatedPath(path, modulePath)

        pathRegistry.savePath(consolidatedPath)
        return consolidatedPath
    }

    private fun makePath(
        moduleDefinition: ModuleDefinition,
        parentModuleDefinition: ModuleDefinition?
    ) = pathRegistry.makePath(moduleDefinition.path, parentModuleDefinition?.path)

    private fun getConsolidatedPath(
        path: Path,
        modulePath: Path
    ): Path {
        return if (path != Path.root()) modulePath.copy(parent = path) else modulePath
    }

    private fun updateDefinition(
        definition: BeanDefinition<*>,
        name: String,
        eager: Boolean,
        override: Boolean,
        consolidatedPath: Path
    ): BeanDefinition<out Any?> {
        return definition.copy(
            name = name,
            options = BeanDefinitionOptions(
                isEager = eager,
                allowOverride = override
            ),
            path = consolidatedPath
        )
    }

    private fun defineDefinitionName(
        definition: BeanDefinition<*>,
        consolidatedPath: Path
    ): String {
        val name = definition.name
        return if (name.isEmpty()) {
            val scopeString = definition.getScopeName()
            val pathString = consolidatedPath.getConsolidatedPath()
            val className = definition.primaryType.simpleName()
            "$scopeString$pathString$className"
        } else name
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
            loadPropertiesFromFile()
        }

        if (props.extraProperties.isNotEmpty()) {
            loadProperties(props.extraProperties)
        }

        if (props.useEnvironmentProperties) {
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
            Koin.logger?.debug("[properties] loaded $nb properties from '$koinFile' file")
        }
    }

    /**
     * Inject all system properties to context
     */
    private fun loadEnvironmentProperties() {
        val n1 = propertyResolver.import(System.getProperties())
        Koin.logger?.debug("[properties] loaded $n1 properties from properties")
        val n2 = propertyResolver.import(System.getenv().toProperties())
        Koin.logger?.debug("[properties] loaded $n2 properties from env properties")
    }

    /**
     * Register ScopeCallback - being notified on Scope closing
     * @see ScopeCallback - ScopeCallback
     */
    fun registerScopeCallback(callback: ScopeCallback) {
        scopeRegistry.register(callback)
    }

    /**
     * Register ModuleCallBack - being notified on Path releaseInstance
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

