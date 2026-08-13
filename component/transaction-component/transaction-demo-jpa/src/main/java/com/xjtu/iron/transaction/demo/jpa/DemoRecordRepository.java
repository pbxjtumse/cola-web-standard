package com.xjtu.iron.transaction.demo.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DemoRecordRepository extends JpaRepository<DemoRecordEntity, Long> {
}
