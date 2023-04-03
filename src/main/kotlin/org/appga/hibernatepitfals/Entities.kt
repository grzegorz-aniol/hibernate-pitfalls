package org.appga.hibernatepitfals

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Inheritance
import javax.persistence.InheritanceType
import javax.persistence.JoinColumn
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.OneToOne
import javax.persistence.QueryHint
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@NamedQuery(name = "findAllCustomers", query = "from Customer", hints = [QueryHint(name = org.hibernate.jpa.QueryHints.HINT_READONLY, value = "true")])
class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    lateinit var id: UUID

    @Column
    lateinit var name: String

    @Column
    lateinit var sinceDate: LocalDate
}

@Entity
@NamedQuery(name = "findAllProducts", query = "from Product", hints = [QueryHint(name = org.hibernate.jpa.QueryHints.HINT_READONLY, value = "true")])
class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    lateinit var id: UUID

    @Column
    lateinit var name: String

    @Column
    lateinit var price: BigDecimal
}

@Entity
class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
