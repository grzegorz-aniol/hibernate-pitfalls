package org.appga.hibernatepitfals

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container

abstract class AbstractPostgresTest {

    companion object {
        @Container
        protected val postgresqlContainer = PostgreSQLContainer<Nothing>()
    }

}