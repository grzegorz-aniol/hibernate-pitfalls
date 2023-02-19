package org.appga.hibernatepitfals

import com.github.javafaker.Faker
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.withinPercentage
import org.jeasy.random.EasyRandom
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.system.measureTimeMillis


@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SlowQueryWithLargeEntitiesLoadedTest {

    companion object {
        @Container
        private val postgresqlContainer = PostgreSQLContainer<Nothing>()

        private var dataInitiated = false
    }

    private val numOfObjects = 10_000
    private val easyRandom = EasyRandom()
    private val faker = Faker()

    @Autowired
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    lateinit var customerRepository: CustomerRepository

    @Autowired
    lateinit var productRepository: ProductRepository

    @BeforeEach
    fun `prepare test data`() {
        if (dataInitiated) {
            return
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
        dataInitiated = true
    }

    private fun runQueries(tries: Int, message: String): Pair<Long, Double> {
        val timeInMs = measureTimeMillis {
            repeat(tries) {
                // searching for none existing entities just to avoid refilling entities in hibernate context
                // each query time should take same time then
                customerRepository.findByName(faker.name().fullName())
                productRepository.findByName(faker.name().fullName())
            }
        }
        val avg = timeInMs.toDouble() / tries.toDouble()
        println("$message. Total time $timeInMs [ms], avg: $avg [ms]")
        return timeInMs to avg
    }

    private fun simulateHeavyDataLoad() {
        val customers = customerRepository.findAll()
        assertThat(customers.size).isEqualTo(numOfObjects)
        val products = productRepository.findAll()
        assertThat(products.size).isEqualTo(numOfObjects)
        println("Loaded ${customers.size + products.size} entities to the session")
    }

    @Test
    @Order(1)
    fun `problem - running query with hibernate containing multiple entities is slow - this will fail`() {
        val tries = 1_000
        transactionTemplate.execute {
            // reading before
            val (_, searchingAvgTime) = runQueries(tries, "Searching time w/o entities in memory")

            simulateHeavyDataLoad()

            // reading after
            val (_, searchingAvgTimeAfter ) = runQueries(tries, "Searching time with loaded entities")
            assertThat(searchingAvgTime)
                .`as`("Queries should be as much performant as before with 20% tolerance")
                .isCloseTo(searchingAvgTimeAfter, withinPercentage(20))
        }
    }

    @Test
    @Order(2)
    fun `solution 1 - clearing hibernate context after heavy load`() {
        val tries = 1_000
        transactionTemplate.execute {
            // reading before
            val (_, searchingAvgTime) = runQueries(tries, "Searching time w/o entities in memory")

            simulateHeavyDataLoad()

            // The only change is to clear the context. This will avoid dirty check on loaded entities before each query
            // We can skip entityManager.flush in this case, as the transaction is read-only - no changes are made
            entityManager.clear()

            // reading after
            val (_, searchingAvgTimeAfter ) = runQueries(tries, "Searching time with loaded entities")
            assertThat(searchingAvgTime)
                .`as`("Queries should be as much performant as before with 20% tolerance")
                .isCloseTo(searchingAvgTimeAfter, withinPercentage(20))
        }
    }

    @Test
    @Order(3)
    fun `solution 2 - running next query in another hibernate session (transaction)`() {
        val tries = 1_000

        // transaction #1
        val searchingAvgTime = transactionTemplate.execute {
            // reading before
            val (_, avg) = runQueries(tries, "Searching time w/o entities in memory")
            simulateHeavyDataLoad()
            avg
        }

        // transaction #2
        val searchingAvgTimeAfter = transactionTemplate.execute {
            // reading after
            val (_, avg ) = runQueries(tries, "Searching time with loaded entities")
            avg
        }

        assertThat(searchingAvgTime)
            .`as`("Queries should be as much performant as before with 20% tolerance")
            .isCloseTo(searchingAvgTimeAfter, withinPercentage(20))
    }

}