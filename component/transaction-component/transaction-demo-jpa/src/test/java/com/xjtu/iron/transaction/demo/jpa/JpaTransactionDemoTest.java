package com.xjtu.iron.transaction.demo.jpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JpaTransactionDemoTest {

    @Autowired JpaTransactionDemoService service;
    @Autowired DemoRecordRepository repository;

    @Test
    void jpaShouldCommitAndRollbackThroughTransactionExecutor() {
        long before = repository.count();
        service.commit(1L);
        assertEquals(before + 1, repository.count());

        assertThrows(IllegalStateException.class, () -> service.rollback(2L));
        assertEquals(before + 1, repository.count());
    }

    @Test
    void requiresNewShouldCommitInnerIndependently() {
        long before = repository.count();
        assertThrows(IllegalStateException.class, service::outerRollbackInnerRequiresNew);
        assertEquals(before + 1, repository.count());
        assertEquals("inner", repository.findById(2002L).orElseThrow().getName());
    }
}
