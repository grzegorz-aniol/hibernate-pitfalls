package org.appga.hibernatepitfals

import java.util.UUID
import org.appga.hibernatepitfals.hibernate.DirtyCheckInterceptor
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.engine.spi.SessionImplementor
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.system.measureTimeMillis


@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(value = MethodOrderer.MethodName::class)
class SlowQueryWithMultipleEntitiesLoadedTest : AbstractPostgresTest() {

    @Autowired
    lateinit var readOnlyWrapper: ReadOnlyWrapperService

    @BeforeAll
    fun setupData() {
        createTestData()
    }

    @Test
    fun `problem - running query with hibernate containing multiple entities is slow - this will fail`() {
        val tries = 1_000
        transactionTemplate.execute {
            // reading before
            val (_, searchingAvgTime) = runQueries(tries, "Searching time w/o entities in memory")

            runHeavyDataLoad()

            // reading after
            val (_, searchingAvgTimeAfter) = runQueries(tries, "Searching time with loaded entities")
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

            runHeavyDataLoad()

            // The only change is to clear the context. This will avoid dirty check on loaded entities before each query
            // We can skip entityManager.flush in this case, as the transaction is read-only - no changes are made
            val cleanTimeMs = measureTimeMillis {
                entityManager.clear()
            }
            log.info("Time spent on entity manager clear: $cleanTimeMs [ms]")

            // reading after
            val (_, searchingAvgTimeAfter) = runQueries(tries, "Searching time with loaded entities")
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
            runHeavyDataLoad()
            avg
        }

        // transaction #2
        val searchingAvgTimeAfter = transactionTemplate.execute {
            // reading after
            val (_, avg) = runQueries(tries, "Searching time with loaded entities")
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

            runHeavyDataLoadReadOnly()

            // reading after
            val (_, searchingAvgTimeAfter) = runQueries(tries, "Searching time with loaded entities")
            assertThat(searchingAvgTimeAfter)
                .`as`("Queries should be as much performant as before with some tolerance")
                .isLessThan((1.0 + tolerance) * searchingAvgTime)
        }
    }

    @Test
    @Transactional
    fun `solution 4 - perform heavy load using read-only context`() {
        val tries = 1000

        // reading before
        val (_, searchingAvgTime) = runQueries(tries, "Searching time w/o entities in memory")

        val result = runHeavyDataLoadAsReadOnly()
        val entitiesCount = getPersistenceContextEntitiesCount()
        log.info("Number of entities in the context: $entitiesCount")

        // reading after
        DirtyCheckInterceptor.getAndReset()
        val (_, searchingAvgTimeAfter) = runQueries(tries, "Searching time with loaded entities")
        val dirtyChecks = DirtyCheckInterceptor.getAndReset()
        log.info("Dirty checks: $dirtyChecks")
        assertThat(dirtyChecks).isZero()

        // unfortunately this method is not as much performant as the other solution
        // still the same queries are few times slower
        assertThat(searchingAvgTimeAfter)
            .`as`("Queries should be as much performant as before with some tolerance")
            .isLessThan(6.0 * searchingAvgTime)
    }

    private fun getPersistenceContextEntitiesCount(): Int {
        return entityManager.unwrap(SessionImplementor::class.java).persistenceContext.reentrantSafeEntityEntries().size
    }

    @Test
    fun `solution 5 - explicit eviction of loaded entities`() {
        val tries = 1_000

        transactionTemplate.execute {
            // reading before
            val (_, searchingAvgTime) = runQueries(tries, "Searching time w/o entities in memory")

            val result = runHeavyDataLoad()
            val detachTime = measureTimeMillis {
                detachAll(result.customers)
                detachAll(result.products)
            }
            log.info("Time spent on detaching: $detachTime [ms]")

            // reading after
            val (_, searchingAvgTimeAfter) = runQueries(tries, "Searching time with loaded entities")
            assertThat(searchingAvgTimeAfter)
                .`as`("Queries should be as much performant as before with some tolerance")
                .isLessThan((1.0 + tolerance) * searchingAvgTime)
        }
    }

    @Test
    fun `solution 6 - run heavy load via TX NOT_SUPPORTED propagation method`() {
        val tries = 1_000

        transactionTemplate.execute {
            // reading before
            val (_, searchingAvgTime) = runQueries(tries, "Searching time w/o entities in memory")

            runHeavyDataLoadWithTxNotSupported()

            // reading after
            val (_, searchingAvgTimeAfter) = runQueries(tries, "Searching time with loaded entities")
            assertThat(searchingAvgTimeAfter)
                .`as`("Queries should be as much performant as before with some tolerance")
                .isLessThan((1.0 + tolerance) * searchingAvgTime)
        }
    }

    private fun runQueries(tries: Int, message: String): Pair<Long, Double> {
        val timeInMs = measureTimeMillis {
            repeat(tries) {
                // searching for none existing entities just to avoid loading entities to hibernate context
                // so each query time should be same
                customerRepository.findByName(UUID.randomUUID().toString())
                productRepository.findByName(UUID.randomUUID().toString())
            }
        }
        val avg = timeInMs.toDouble() / tries.toDouble()
        log.info("$message. Total time $timeInMs [ms], avg: $avg [ms]")
        return timeInMs to avg
    }

    private fun runHeavyDataLoad(): AllDataResult {
        val customers = customerRepository.findAll()
        assertThat(customers.size).isEqualTo(numOfObjects)
        val products = productRepository.findAll()
        assertThat(products.size).isEqualTo(numOfObjects)
        log.info("Loaded ${customers.size + products.size} entities to the session")
        return AllDataResult(customers = customers, products = products)
    }

    private fun runHeavyDataLoadReadOnly() {
        val (customers, products) = readOnlyWrapper.findAllCustomersAndProductsInNestedTx()
        assertThat(customers.size).isEqualTo(numOfObjects)
        assertThat(products.size).isEqualTo(numOfObjects)
        log.info("Loaded ${customers.size + products.size} entities to the session")
    }

    private fun runHeavyDataLoadAsReadOnly(): AllDataResult {
        val (customers, products) = readOnlyWrapper.findAllInReadOnlyContext()
        assertThat(customers.size).isEqualTo(numOfObjects)
        assertThat(products.size).isEqualTo(numOfObjects)
        log.info("Loaded ${customers.size + products.size} entities to the session")
        return AllDataResult(customers = customers, products = products)
    }

    private fun runHeavyDataLoadWithTxNotSupported(): AllDataResult {
        val (customers, products) = readOnlyWrapper.findAllCustomersAndProductsWithTxNotSupported()
        assertThat(customers.size).isEqualTo(numOfObjects)
        assertThat(products.size).isEqualTo(numOfObjects)
        log.info("Loaded ${customers.size + products.size} entities to the session")
        return AllDataResult(customers = customers, products = products)
    }

    private fun <T> detachAll(obj: Iterable<T>) {
        obj.forEach { entityManager.detach(it) }
    }

}