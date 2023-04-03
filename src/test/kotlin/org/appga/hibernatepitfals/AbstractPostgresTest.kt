package org.appga.hibernatepitfals

import com.github.javafaker.Faker
import javax.persistence.EntityManager
import org.jeasy.random.EasyRandom
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container

abstract class AbstractPostgresTest() {

    @Autowired
    protected lateinit var entityManager: EntityManager

    @Autowired
    protected lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    protected lateinit var productRepository: ProductRepository

    @Autowired
    protected lateinit var customerReadOnlyRepository: CustomerReadOnlyRepository

    @Autowired
    protected lateinit var customerRepository: CustomerRepository

    companion object {
        @Container
        @JvmStatic
        protected val postgresqlContainer = PostgreSQLContainer<Nothing>()
        @JvmStatic
        protected val log: Logger = LoggerFactory.getLogger(this::class.java)
        @JvmStatic
        private var dataInitiated = false
    }

    protected val tolerance = 0.33
    protected val numOfObjects = 10_000
    protected val easyRandom = EasyRandom()
    protected val faker = Faker()

    fun createTestData() {
        if (dataInitiated) {
            return
        }
        transactionTemplate.execute {
            if (customerRepository.count() >= numOfObjects && productRepository.count() >= numOfObjects) {
                log.warn("Test data already exists")
                return@execute
            }
            // create test data
            repeat(numOfObjects) {
                customerRepository.save(easyRandom.nextObject(Customer::class.java))
                productRepository.save(easyRandom.nextObject(Product::class.java))
            }
            // warm up
            repeat(numOfObjects) {
                customerRepository.findByName(faker.name().fullName())
                productRepository.findByName(faker.name().fullName())
            }
        }
        dataInitiated = true
    }


}