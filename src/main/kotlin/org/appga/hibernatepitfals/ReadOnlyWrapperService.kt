package org.appga.hibernatepitfals

import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

data class AllDataResult(val customers: List<Customer>, val products: List<Product>)
data class AllImmutableDataResult(val customers: List<ImmutableCustomer>, val products: List<ImmutableProduct>)

@Service
class ReadOnlyWrapperService(
    private val entityManager: EntityManager,
    private val customerRepository: CustomerRepository,
    private val customerReadOnlyRepository: CustomerReadOnlyRepository,
    private val productRepository: ProductRepository,
    private val productReadOnlyRepository: ProductReadOnlyRepository,
    private val immutableCustomerRepository: ImmutableCustomerRepository,
    private val immutableProductRepository: ImmutableProductRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun findAllCustomersAndProductsInNestedTx() = AllDataResult(
        customers = customerRepository.findAll(),
        products = productRepository.findAll()
    )

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun findAllCustomersAndProductsWithTxNotSupported() = AllDataResult(
        customers = customerRepository.findAll(),
        products = productRepository.findAll()
    )

    @Transactional(propagation = Propagation.MANDATORY)
    fun findAllInReadOnlyContext(): AllImmutableDataResult {
        val session = entityManager.unwrap(Session::class.java)
        val prevState = session.isDefaultReadOnly
        try {
            session.isDefaultReadOnly = true
            return AllImmutableDataResult(
                  customers = immutableCustomerRepository.findAll(),
                  products = immutableProductRepository.findAll(),
            )
        } finally {
            session.isDefaultReadOnly = prevState
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun findAllByQuery(): AllDataResult {
        return AllDataResult(
            customers = entityManager.createQuery("select c from Customer c", Customer::class.java)
                .setHint(org.hibernate.jpa.QueryHints.HINT_READONLY, true)
                .resultList,
            products = entityManager.createQuery("select p from Product p", Product::class.java)
                .setHint(org.hibernate.jpa.QueryHints.HINT_READONLY, true)
                .resultList
        )
    }
}