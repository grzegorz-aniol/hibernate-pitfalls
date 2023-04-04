package org.appga.hibernatepitfals

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface ImmutableCustomerRepository : JpaRepository<ImmutableCustomer, UUID>

interface ImmutableProductRepository : JpaRepository<ImmutableProduct, UUID>
