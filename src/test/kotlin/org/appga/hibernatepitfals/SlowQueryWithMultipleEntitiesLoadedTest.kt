package org.appga.hibernatepitfals

import com.github.javafaker.Faker
import javax.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.jeasy.random.EasyRandom
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.support.TransactionTemplate
import kotlin.system.measureTimeMillis


@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SlowQueryWithMultipleEntitiesLoadedTest : AbstractPostgresTest() {

    companion object {
        private var dataInitiated = false
    }

    private val tolerance = 0.33
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

    @Autowired
    lateinit var readOnlyWrapper : ReadOnlyWrapperService

    @Test
    fun `problem - running query with hibernate containing multiple entities is slow - this will fail`() {
        val tries = 1_000
        transactionTemplate.execute {
            // reading before
            val (_, searchingAvgTime) = runQueries(tries, "Searching time w/o entities in memory")

            simulateHeavyDataLoad()

            // reading after
            val (_, searchingAvgTimeAfter ) = runQueries(tries, "Searching time with loaded entities")
            assertThat(searchingAvgTimeAfter)
                .`as`("Queries should be as much performant as before with some tolerance")
                .isLessThan((1.0 + tolerance) * searchingAvgTime)
        }
    }

    @Test
    fun `solution 1 - clearing hibernate context after heavy load`() {
        val tries = 1_000
        transactionTemplate.execute {
            // reading before
            val (_, searchingAvgTime) = runQueries(tries, "Searching time w/o entities in memory")

            simulateHeavyDataLoad()

            // The only change is to clear the context. This will avoid dirty check on loaded entities before each query
            // We can skip entityManager.flush in this case, as the transaction is read-only - no changes are made
            val cleanTimeMs = measureTimeMillis {
                entityManager.clear()
            }
            println("Time spent on entity manager clear: $cleanTimeMs [ms]")

            // reading after
            val (_, searchingAvgTimeAfter ) = runQueries(tries, "Searching time with loaded entities")
            assertThat(searchingAvgTimeAfter)
                .`as`("Queries should be as much performant as before with some tolerance")
                .isLessThan((1.0 + tolerance) * searchingAvgTime)
        }
    }

    @Test
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

        assertThat(searchingAvgTimeAfter!!)
            .`as`("Queries should be as much performant as before with some tolerance")
            .isLessThan((1.0 + tolerance) * searchingAvgTime!!)
    }

    @Test
    fun `solution 3 - perform heavy load using nested read-only transaction`() {
        val tries = 1_000

        transactionTemplate.execute {
            // reading before
            val (_, searchingAvgTime) = runQueries(tries, "Searching time w/o entities in memory")

            simulateHeavyDataLoadReadOnly()

            // reading after
            val (_, searchingAvgTimeAfter ) = runQueries(tries, "Searching time with loaded entities")
            assertThat(searchingAvgTimeAfter)
                .`as`("Queries should be as much performant as before with some tolerance")
                .isLessThan((1.0 + tolerance) * searchingAvgTime)
        }
    }

    @Test
    fun `try #5 - it doesn't work - perform heavy load using JPA queries with read-only hint`() {
        val tries = 100

        transactionTemplate.execute {
            // reading before
            val (_, searchingAvgTime) = runQueries(tries, "Searching time w/o entities in memory")

            val result = simulateHeavyDataLoadByJpaQueries()

            // try to modify an entity
            val c1 = result.customers.first()
            c1.name = "test"
            customerRepository.saveAndFlush(c1)

            // reading after
            val (_, searchingAvgTimeAfter ) = runQueries(tries, "Searching time with loaded entities")
            assertThat(searchingAvgTimeAfter)
                .`as`("Queries should be as much performant as before with some tolerance")
                .isLessThan((1.0 + tolerance) * searchingAvgTime)
        }
    }

    @Test
    fun `solution 6 - explicit eviction of loaded entities`() {
        val tries = 1_000

        transactionTemplate.execute {
            // reading before
            val (_, searchingAvgTime) = runQueries(tries, "Searching time w/o entities in memory")

            val result = simulateHeavyDataLoad()
            val detachTime = measureTimeMillis {
                detachAll(result.customers)
                detachAll(result.products)
            }
            println("Time spent on detaching: $detachTime [ms]")

            // reading after
            val (_, searchingAvgTimeAfter ) = runQueries(tries, "Searching time with loaded entities")
            assertThat(searchingAvgTimeAfter)
                .`as`("Queries should be as much performant as before with some tolerance")
                .isLessThan((1.0 + tolerance) * searchingAvgTime)
        }
    }

    @Test
    fun `solution 7 - run heavy load via Tx not supported method`() {
        val tries = 1_000

        transactionTemplate.execute {
            // reading before
            val (_, searchingAvgTime) = runQueries(tries, "Searching time w/o entities in memory")

            simulateHeavyDataLoadWithTxNotSupported()

            // reading after
            val (_, searchingAvgTimeAfter ) = runQueries(tries, "Searching time with loaded entities")
            assertThat(searchingAvgTimeAfter)
                .`as`("Queries should be as much performant as before with some tolerance")
                .isLessThan((1.0 + tolerance) * searchingAvgTime)
        }
    }

    @BeforeAll
    fun `prepare test data`() {
        if (dataInitiated) {
            return
        }
        if (customerRepository.count() >= numOfObjects && productRepository.count() >= numOfObjects) {
            println("Test data already exists")
            dataInitiated = true
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

    private fun simulateHeavyDataLoad(): AllDataResult {
        val customers = customerRepository.findAll()
        assertThat(customers.size).isEqualTo(numOfObjects)
        val products = productRepository.findAll()
        assertThat(products.size).isEqualTo(numOfObjects)
        println("Loaded ${customers.size + products.size} entities to the session")
        return AllDataResult(customers = customers, products = products)
    }

    private fun simulateHeavyDataLoadReadOnly() {
        val (customers, products) = readOnlyWrapper.findAllCustomersAndProductsInNestedTx()
        assertThat(customers.size).isEqualTo(numOfObjects)
        assertThat(products.size).isEqualTo(numOfObjects)
        println("Loaded ${customers.size + products.size} entities to the session")
    }

    private fun simulateHeavyDataLoadByJpaQueries(): AllDataResult {
//        val (customers, products) = readOnlyWrapper.findAllByQuery()
        val (customers, products) = readOnlyWrapper.findAllInReadOnlyContext()
        assertThat(customers.size).isEqualTo(numOfObjects)
        assertThat(products.size).isEqualTo(numOfObjects)
        println("Loaded ${customers.size + products.size} entities to the session")
        return AllDataResult(customers = customers, products = products)
    }

    private fun simulateHeavyDataLoadWithTxNotSupported(): AllDataResult {
        val (customers, products) = readOnlyWrapper.findAllCustomersAndProductsWithTxNotSupported()
        assertThat(customers.size).isEqualTo(numOfObjects)
        assertThat(products.size).isEqualTo(numOfObjects)
        println("Loaded ${customers.size + products.size} entities to the session")
        return AllDataResult(customers = customers, products = products)
    }

    private fun <T> detachAll(obj: Iterable<T>) {
        obj.forEach { entityManager.detach(it) }
    }

}