package org.appga.hibernatepitfals

import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReadOnlyEntityTest : AbstractPostgresTest() {

    @Autowired
    lateinit var customerRepository: CustomerReadOnlyRepository

    @Test
    fun `modify read only entity`() {
        val c = customerRepository.findBy().first()
        c.name = "Johny"
        customerRepository.saveAndFlush(c)
        val c2 = customerRepository.findById(c.id).get()
        assertThat(c2.name).isEqualTo("Johny")
    }
}