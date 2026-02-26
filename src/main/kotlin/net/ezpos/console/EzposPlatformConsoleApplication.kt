package net.ezpos.console

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EzposPlatformConsoleApplication

fun main(args: Array<String>) {
    runApplication<EzposPlatformConsoleApplication>(*args)
}

