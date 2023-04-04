package org.appga.hibernatepitfals

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PostUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.Immutable


@Immutable
@Entity
@Table(name = "customer")
class ImmutableCustomer {
    companion object {
        private val log = LoggerFactory.getLogger(Customer::class.java)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @Column
    lateinit var name: String

    @Column
    lateinit var sinceDate: LocalDate

    @PostUpdate
    fun postUpdate() {
        log.info("Customer id $id was updated")
    }
}

@Immutable
@Entity
@Table(name = "product")
class ImmutableProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @Column
    lateinit var name: String

    @Column
    lateinit var price: BigDecimal

}
