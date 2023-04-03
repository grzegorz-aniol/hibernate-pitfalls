package org.appga.hibernatepitfals

import jakarta.persistence.EntityManager
import jakarta.persistence.QueryHint
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository

@Repository
interface CustomerRepository : JpaRepository<Customer, UUID> {
    fun findByName(name: String): Customer?

    @Query(value = "select * from customer", nativeQuery = true)
    @QueryHints(value = [QueryHint(name = org.hibernate.jpa.QueryHints.HINT_READONLY, value = "true")], forCounting = false)
    fun findBy(): List<Customer>
}

@Repository
interface ProductRepository : JpaRepository<Product, UUID> {
    fun findByName(name: String): Product?

    @Query(value = "select * from product", nativeQuery = true)
    @QueryHints(value = [QueryHint(name = org.hibernate.jpa.QueryHints.HINT_READONLY, value = "true")], forCounting = false)
    fun findBy(): List<Product>
}

@Repository
interface PersonRepository : JpaRepository<Person, UUID> {
    @EntityGraph(attributePaths = ["address"])
    override fun findAll(): List<Person>
}