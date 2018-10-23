package org.koin.example

import org.koin.core.time.measureDuration
import org.koin.log.PrintLogger
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.inject


class CoffeeApp : KoinComponent {

    val maker: CoffeeMaker by inject()
}


fun main(vararg args: String) {
    startKoin(
        list = listOf(coffeeAppModule),
        logger = PrintLogger(showDebug = true)
    )

    val appDuration = measureDuration {
        CoffeeApp().maker.brew()
    }
    println("App run in $appDuration ms")
}
