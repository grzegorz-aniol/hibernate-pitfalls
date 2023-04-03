package org.appga.hibernatepitfals.hibernate

import java.io.Serializable
import org.hibernate.EmptyInterceptor
import org.hibernate.Transaction
import org.hibernate.type.Type
import org.slf4j.LoggerFactory

class DirtyCheckInterceptor : EmptyInterceptor() {

    private val log = LoggerFactory.getLogger(DirtyCheckInterceptor::class.java)

    override fun findDirty(
        entity: Any?,
        id: Serializable?,
        currentState: Array<out Any>?,
        previousState: Array<out Any>?,
        propertyNames: Array<out String>?,
        types: Array<out Type>?
    ): IntArray? {
        if (entity != null) {
            log.info("Checking dirty check for entity: ${entity.javaClass.simpleName} with id: $id")
            return super.findDirty(entity, id, currentState, previousState, propertyNames, types)
        }
        return null
    }

    override fun afterTransactionBegin(tx: Transaction?) {
        log.info("Starting transaction >>>>>>>>>>>>>>>>>>>>>>>>>>")
        super.afterTransactionBegin(tx)
    }

    override fun afterTransactionCompletion(tx: Transaction?) {
        log.info("Transaction completed <<<<<<<<<<<<<<<<<<<<<<<<<")
        super.afterTransactionCompletion(tx)
    }
}