tasks.register("printModules") {
    group = "help"
    description = "Lists the modules that make up the Lightbridge Keycloak SPI."
    val names = subprojects.map { it.path }
    doLast {
        names.forEach { println(it) }
    }
}
