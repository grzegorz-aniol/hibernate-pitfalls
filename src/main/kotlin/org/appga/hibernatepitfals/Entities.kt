package org.appga.hibernatepitfals

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
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

@Entity
class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    lateinit var id: UUID

    @Column
    lateinit var street: String

    @Column
    lateinit var city: String

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_uuid")
    lateinit var person: Person
}

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
open class Human {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    open lateinit var id: UUID

    open lateinit var dna: String
}

@Entity
class Person : Human() {

    @Column
    lateinit var name: String

    @OneToOne(fetch = FetchType.EAGER, cascade = [CascadeType.ALL], mappedBy = "person")
//    @Fetch(value = FetchMode.JOIN)
    lateinit var address: Address
}
