package org.liamjd.cantilever.api.controllers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.inject

class ComponentA
class ComponentB(val a: ComponentA)

class MockTestTest : KoinTest {

    // Lazy inject property
    val componentB: ComponentB by inject()




    @Test
    fun `should inject my components`() {
        startKoin {
            modules(
                module {
                    single { ComponentA() }
                    single { ComponentB(get()) }
                })
        }

        // directly request an instance
        val componentA = get<ComponentA>()

        assertNotNull(componentA)
        assertEquals(componentA, componentB.a)
    }
}