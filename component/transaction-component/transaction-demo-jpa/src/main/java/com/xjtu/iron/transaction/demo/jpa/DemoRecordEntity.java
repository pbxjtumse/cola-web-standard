package com.xjtu.iron.transaction.demo.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tx_demo")
public class DemoRecordEntity {

    @Id
    private Long id;

    private String name;

    protected DemoRecordEntity() {
    }

    public DemoRecordEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
