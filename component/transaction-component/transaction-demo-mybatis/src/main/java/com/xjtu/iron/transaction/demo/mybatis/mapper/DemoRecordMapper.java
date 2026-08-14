package com.xjtu.iron.transaction.demo.mybatis.mapper;

import com.xjtu.iron.transaction.demo.mybatis.domain.DemoRecord;

/**
 * MyBatis Mapper 接口。
 *
 * <p>这里不使用 {@code @Insert/@Select} SQL 注解，所有 SQL 都位于 resources/mapper/DemoRecordMapper.xml。</p>
 */
public interface DemoRecordMapper {

    int insert(DemoRecord record);

    int count();

    String findName(long id);

    int deleteAll();
}
