rootProject.name = "lightbridge-keycloak-spi"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include(
    ":modules:spi-common",
    ":modules:context-client",
    ":modules:token-exchange",
    ":modules:protocol-mapper",
    ":dist",
    ":integration-tests",
)
