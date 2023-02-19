package org.appga.hibernatepitfals

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @Column
    lateinit var name: String

    @Column
    lateinit var sinceDate: LocalDate
}

@Entity
class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @Column
    lateinit var name: String

    @Column
    lateinit var price: BigDecimal
}