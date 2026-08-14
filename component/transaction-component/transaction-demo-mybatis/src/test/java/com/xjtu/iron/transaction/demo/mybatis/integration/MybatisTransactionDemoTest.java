package com.xjtu.iron.transaction.demo.mybatis.integration;

import com.xjtu.iron.transaction.demo.mybatis.application.MybatisTransactionDemoApplication;
import com.xjtu.iron.transaction.demo.mybatis.mapper.DemoRecordMapper;
import com.xjtu.iron.transaction.demo.mybatis.service.MybatisTransactionDemoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = MybatisTransactionDemoApplication.class)
@ActiveProfiles("test")
class MybatisTransactionDemoTest {

    @Autowired
    private MybatisTransactionDemoService service;

    @Autowired
    private DemoRecordMapper mapper;

    @BeforeEach
    void cleanTable() {
        // 每个测试场景前清空 Demo 表，保证不同事务流程之间互不污染。
        mapper.deleteAll();
    }

    @Test
    void successfulCallbackShouldCommit() {
        // 执行一个正常事务。
        service.commit(1L);

        // callback 正常结束后数据应当已经提交。
        assertEquals(1, mapper.count());
        assertEquals("commit", mapper.findName(1L));
    }

    @Test
    void businessExceptionShouldRollback() {
        // callback 在 INSERT 后抛异常，事务组件应保留原异常并回滚 INSERT。
        assertThrows(IllegalStateException.class, () -> service.rollback(2L));

        // 数据库中不能留下本次失败事务的数据。
        assertEquals(0, mapper.count());
    }

    @Test
    void requiresNewShouldCommitInnerIndependently() {
        // outer 最终会失败，但 inner 使用 REQUIRES_NEW 独立提交。
        assertThrows(IllegalStateException.class, service::outerRollbackInnerRequiresNew);

        // outer 记录被回滚，只留下 inner 记录。
        assertEquals(1, mapper.count());
        assertEquals("inner", mapper.findName(1002L));
    }

    @Test
    void requiredInnerShouldRollbackWithOuter() {
        // inner REQUIRED 加入 outer 的同一物理事务；outer 失败时两条记录一起回滚。
        assertThrows(IllegalStateException.class, service::outerRollbackInnerRequired);
        assertEquals(0, mapper.count());
    }

    @Test
    void rollbackOnlyShouldRollbackWithoutBusinessException() {
        // callback 没有抛异常，只调用 setRollbackOnly()。
        service.rollbackOnly(3L);

        // Provider 在完成阶段识别 rollback-only，因此记录不会提交。
        assertEquals(0, mapper.count());
    }
}
