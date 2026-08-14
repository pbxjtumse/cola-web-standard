package com.xjtu.iron.transaction.demo.jpa.repository;

import com.xjtu.iron.transaction.demo.jpa.domain.DemoRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA Repository。
 */
public interface DemoRecordRepository extends JpaRepository<DemoRecordEntity, Long> {
}
