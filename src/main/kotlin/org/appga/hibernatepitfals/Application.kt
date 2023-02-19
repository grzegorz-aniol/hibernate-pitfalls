package org.appga.hibernatepitfals

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories(basePackageClasses = [Application::class])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
