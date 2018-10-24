package org.koin.test.module

import org.junit.Assert
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.get
import org.koin.test.AutoCloseKoinTest
import org.koin.test.ext.junit.assertDefinitions

class ImplicitNamingTest : AutoCloseKoinTest() {

    val module = module {
        module("B") {
            single { ComponentA() }
            single { ComponentB(get()) }
        }

        module("C") {
            single { ComponentA() }
            single { ComponentC(get()) }
        }
    }

    val conflictNames = module {
        module("C") {
            single<Contract1.Presenter> { ComponentD() }
            single<Contract2.Presenter> { ComponentE() }
        }
    }

    class ComponentA
    class ComponentB(val componentA: ComponentA)
    class ComponentC(val componentA: ComponentA)
    class ComponentD : Contract1.Presenter
    class ComponentE : Contract2.Presenter

    interface Contract1 {
        interface Presenter
    }

    interface Contract2 {
        interface Presenter
    }

    @Test
    fun `declared module from classes`() {
        startKoin(listOf(module))

        assertDefinitions(4)

        Assert.assertNotNull(get<ComponentB>())
        Assert.assertNotNull(get<ComponentC>())

        val a_b = get<ComponentA>(name = "B.ComponentA")
        val a_c = get<ComponentA>(name = "C.ComponentA")
        assertNotEquals(a_b, a_c)
    }

    @Test
    fun `resolve conflicting names`() {
        startKoin(listOf(conflictNames))

        assertDefinitions(2)

        get<Contract1.Presenter>()
        get<Contract2.Presenter>()

        get<Contract1.Presenter>(name = "C.Presenter")

    }
}