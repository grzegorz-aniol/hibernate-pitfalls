package org.appga.hibernatepitfals

import com.github.javafaker.Faker
import org.appga.hibernatepitfals.hibernate.DirtyCheckInterceptor
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.Session
import org.hibernate.jpa.QueryHints
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReadOnlyEntityTest : AbstractPostgresTest() {

    @BeforeAll
    fun setupTest() {
        createTestData()
    }

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
    @Transactional
    fun `should not modify object if session is read only (variant with utility class)`() {
        val newName = Faker.instance().funnyName().name()
        log.info("New name: $newName")

        DirtyCheckInterceptor.getAndReset()

        val customerId = withReadOnlyEntities {
            val c = customerRepository.findFirstBy()
            c.name = newName
            entityManager.flush()
            entityManager.clear()
            c.id
        }

        val dirtyChecks = DirtyCheckInterceptor.getAndReset()
        assertThat(dirtyChecks).isZero()

        val c2 = customerReadOnlyRepository.findById(customerId).get()
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

        val c = customerRepository.findFirstBy()
        c.name = newName
        entityManager.flush()
        entityManager.clear()

        val c2 = customerReadOnlyRepository.findById(c.id).get()
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

    @Test
    @Transactional
    @Disabled("Dirty check is not run in that case with Hibernate 6")
    fun `should run dirty check before any query`() {
        // fetch all entities
        customerRepository.findAll().first()

        DirtyCheckInterceptor.getAndReset()
        customerRepository.findByName("xxxx")
        val dirtyChecks = DirtyCheckInterceptor.getAndReset()
        assertThat(dirtyChecks).isEqualTo(10_000)
    }


    @Test
    @Transactional
    fun `should not run dirty check if entities were loaded in read-only mode`() {
        // load all entities to persistence context (using read-only hint)
        customerReadOnlyRepository.findBy()

        DirtyCheckInterceptor.getAndReset()
        customerRepository.findByName("xxxx")
        val dirtyChecks = DirtyCheckInterceptor.getAndReset()
        assertThat(dirtyChecks).isZero()
    }

    @Test
    @Transactional
    fun `should not run dirty check if entities were loaded in read-only mode (variant #2)`() {
        withReadOnlyEntities {
            // load all entities to Persistence Context as read-only
            customerRepository.findAll().first()
        }

        DirtyCheckInterceptor.getAndReset()
        customerRepository.findByName("xxxx")
        val dirtyChecks = DirtyCheckInterceptor.getAndReset()
        assertThat(dirtyChecks).isZero()
    }

    fun <T> withReadOnlyEntities(producer: () -> T): T {
        val session = entityManager.unwrap(Session::class.java)
        val prevReadOnlyValue = session.isDefaultReadOnly
        try {
            session.isDefaultReadOnly = true
            return producer()
        } finally {
            session.isDefaultReadOnly = prevReadOnlyValue
        }
    }
}