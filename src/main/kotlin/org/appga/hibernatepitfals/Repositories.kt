package org.appga.hibernatepitfals

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CustomerRepository : JpaRepository<Customer, UUID> {
    fun findByName(name: String): Customer?
}

@Repository
interface ProductRepository : JpaRepository<Product, UUID> {
    fun findByName(name: String): Product?
}
