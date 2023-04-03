package org.appga.hibernatepitfals

import org.assertj.core.api.Assertions.assertThat
import org.jeasy.random.EasyRandom
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonLoadTest : AbstractPostgresTest() {

    private val log = LoggerFactory.getLogger(PersonLoadTest::class.java)

    @Autowired
    lateinit var personRepository: PersonRepository

    lateinit var p1: Person
    lateinit var p2: Person

    private val easyRandom = EasyRandom()

    @Test
    @Order(1)
    fun `create persons`() {
        p1 = personRepository.save(Person().also {
            it.name = "John"
            it.dna = "test"
            it.address =  easyRandom.nextObject(Address::class.java)
        })
        p2 = personRepository.save(Person().also {
            it.name = "Alice"
            it.dna = "test"
            it.address = easyRandom.nextObject(Address::class.java)
        })
    }

    @Test
    @Order(2)
    fun `load person by id`() {
        log.info("Load person by ID")
        val p = personRepository.findById(p1.id).get()
        assertThat(p.name).isEqualTo("John")
    }

    @Test
    @Order(3)
    fun `load persons by query`() {
        log.info("Load all persons")
        val result = personRepository.findAll();
        assertThat(result).hasSize(2)
    }
}