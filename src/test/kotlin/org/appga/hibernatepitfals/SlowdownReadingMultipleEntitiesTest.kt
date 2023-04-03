package org.appga.hibernatepitfals

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage.withPercentage
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.nield.kotlinstatistics.percentile
import org.springframework.boot.test.context.SpringBootTest
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SlowdownReadingMultipleEntitiesTest : AbstractPostgresTest() {

    @Test
    @Order(1)
    fun `problem - running query with full hibernate context loaded is less performant - this will fail`() {
        val metrics = transactionTemplate.execute {
            loadEntitiesByQuery(
                clearEvery = Int.MAX_VALUE, // never clear context
                message = "Loading data without cleanup"
            )
        }
        assertThat(metrics?.p90)
            .`as`("Queries time should be uniform")
            .isCloseTo(metrics?.avg, withPercentage(25.0))
    }

    @Test
    @Order(2)
    fun `solution - clear often the context`() {
        val metrics = transactionTemplate.execute {
            loadEntitiesByQuery(
                clearEvery = 100,
                message = "Loading data with cleanup"
            )
        }
        assertThat(metrics?.p90)
            .`as`("Queries time should be uniform")
            .isCloseTo(metrics?.avg, withPercentage(25.0))
    }

    @BeforeAll
    fun `prepare test data`() {
        createTestData()
    }

    private data class Metrics(val totalTime: Long, val avg: Double, val p90: Double)

    private fun loadEntitiesByQuery(clearEvery: Int, message: String): Metrics {
        val results = ArrayList<Double>(numOfObjects)
        val timeInMs = measureTimeMillis {
            repeat(numOfObjects) { num ->
                val opTimeInMs = 1e-6 * measureNanoTime {
                    customerRepository.findByName("customer_$num")
                    productRepository.findByName("product_$num")
                }
                results.add(opTimeInMs)
                if (results.size % clearEvery == 0) {
                    entityManager.clear()
                }
            }
        }
        val avg = results.average()
        val p90 = results.percentile(0.9)
        println("$message. Total time $timeInMs [ms], avg: $avg [ms], p90=$p90")
        return Metrics(totalTime = timeInMs, avg = avg, p90 = p90)
    }

}