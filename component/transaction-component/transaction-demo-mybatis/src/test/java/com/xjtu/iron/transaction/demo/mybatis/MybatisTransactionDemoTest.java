package com.xjtu.iron.transaction.demo.mybatis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MybatisTransactionDemoTest {

    @Autowired MybatisTransactionDemoService service;
    @Autowired DemoRecordMapper mapper;

    @Test
    void mybatisShouldCommitAndRollbackThroughTransactionExecutor() {
        int before = mapper.count();
        service.commit(1L);
        assertEquals(before + 1, mapper.count());

        assertThrows(IllegalStateException.class, () -> service.rollback(2L));
        assertEquals(before + 1, mapper.count());
    }

    @Test
    void requiresNewShouldCommitInnerIndependently() {
        int before = mapper.count();
        assertThrows(IllegalStateException.class, service::outerRollbackInnerRequiresNew);
        assertEquals(before + 1, mapper.count());
        assertEquals("inner", mapper.findName(1002L));
    }
}
