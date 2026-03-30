package com.example.ejb3.customid;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom ID generator implementing legacy IdentifierGenerator interface.
 * Reproduces the NPE reported in GitHub issue comment on PR #852.
 */
public class SnowflakeIdGenerator implements IdentifierGenerator {

    private static final AtomicLong counter = new AtomicLong(1);

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        return counter.getAndIncrement();
    }
}
