package com.banking.engine.account;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

@Component
public class AccountNumberGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    public String next() {
        Number value = (Number) entityManager.createNativeQuery("select nextval('account_number_seq')").getSingleResult();
        return "BE-" + value.longValue();
    }
}
