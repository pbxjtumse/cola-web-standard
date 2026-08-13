package com.xjtu.iron.transaction.demo.mybatis;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

public interface DemoRecordMapper {

    @Insert("insert into tx_demo(id, name) values(#{id}, #{name})")
    int insert(@Param("id") long id, @Param("name") String name);

    @Select("select count(*) from tx_demo")
    int count();

    @Select("select name from tx_demo where id = #{id}")
    String findName(@Param("id") long id);
}
