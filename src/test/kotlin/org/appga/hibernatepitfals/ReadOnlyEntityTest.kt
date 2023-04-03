package org.appga.hibernatepitfals

import com.github.javafaker.Faker
import javax.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.Session
import org.hibernate.jpa.QueryHints
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReadOnlyEntityTest : AbstractPostgresTest() {

    private val log = LoggerFactory.getLogger(ReadOnlyEntityTest::class.java)

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var customerReadOnlyRepository: CustomerReadOnlyRepository

    @Autowired
    lateinit var customerRepository: CustomerRepository

    @Test
    @Transactional
    fun `should not modify object if session is read only`() {
        val newName = Faker.instance().funnyName().name()
        log.info("New name: $newName")

        val session = entityManager.unwrap(Session::class.java)
        session.isDefaultReadOnly = true
        val c = customerRepository.findFirstBy()
        c.name = newName

        entityManager.flush()
        entityManager.clear()

        val c2 = customerReadOnlyRepository.findById(c.id).get()

        println("is r/o : ${session.isReadOnly(c2)}")
        assertThat(c2.name).isNotEqualTo(newName)
    }

    @Test
    @Transactional(readOnly = true)
    fun `should not modify object if transaction is read only`() {
        val newName = Faker.instance().funnyName().name()
        log.info("New name: $newName")

        val c = customerRepository.findFirstBy()

        c.name = newName

        entityManager.flush()
        entityManager.clear()

        val c2 = customerReadOnlyRepository.findById(c.id).get()

        assertThat(c2.name).isNotEqualTo(newName)
    }


    @Test
    @Transactional
    fun `should modify object if session is NOT read only`() {
        val newName = Faker.instance().funnyName().name()
        log.info("New name: $newName")

        val session = entityManager.unwrap(Session::class.java)
        val c = customerRepository.findFirstBy()
        c.name = newName

        entityManager.flush()
        entityManager.clear()

        val c2 = customerReadOnlyRepository.findById(c.id).get()

        println("is r/o : ${session.isReadOnly(c2)}")
        assertThat(c2.name).isEqualTo(newName)
    }

    @Test
    @Transactional
    fun `should modify object loaded before session is marked as read only`() {
        val newName = Faker.instance().funnyName().name()
        log.info("New name: $newName")

        val session = entityManager.unwrap(Session::class.java)
        val c = customerRepository.findFirstBy()
        c.name = newName

        session.isDefaultReadOnly = true

        entityManager.flush()
        entityManager.clear()

        val c2 = customerReadOnlyRepository.findById(c.id).get()

        println("is r/o : ${session.isReadOnly(c2)}")
        assertThat(c2.name).isEqualTo(newName)
    }

    @Test
    @Transactional
    fun `should modify object loaded before session is marked as read only variant 2`() {
        val newName = Faker.instance().funnyName().name()
        log.info("New name: $newName")

        val session = entityManager.unwrap(Session::class.java)
        val c = customerRepository.findFirstBy()

        session.isDefaultReadOnly = true
        val c2 = customerRepository.findById(c.id).get()
        c2.name = newName

        entityManager.flush()
        entityManager.clear()

        val c3 = customerReadOnlyRepository.findById(c.id).get()

        println("is r/o : ${session.isReadOnly(c3)}")
        assertThat(c3.name).isEqualTo(newName)
    }


    @Test
    @Transactional
    fun `should not update read only entity`() {
        val newName = Faker.instance().funnyName().name()
        log.info("New name: $newName")

        val session = entityManager.unwrap(Session::class.java)
        val c = customerReadOnlyRepository.findBy().first()
        c.name = newName

        entityManager.flush()
        entityManager.clear()

        val c2 = customerReadOnlyRepository.findById(c.id).get()

        println("is r/o : ${session.isReadOnly(c2)}")
        assertThat(c2.name).isNotEqualTo(newName)
    }

    @Test
    @Transactional
    fun `entity loaded by read only hint is not updated`() {
        val newName = Faker.instance().funnyName().name()
        log.info("New name: $newName")

        val session = entityManager.unwrap(Session::class.java)
        val c = session.createQuery("from Customer", Customer::class.java)
            .setHint(QueryHints.HINT_READONLY, "true")
            .resultList.first()
        log.info("is r/o : ${session.isReadOnly(c)}")
        c.name = newName

        entityManager.flush()
        entityManager.clear()

        val c2 = session.get(Customer::class.java, c.id)
        assertThat(c2.name).isNotEqualTo(newName)
    }
}