package com.example.multischema.auction;

import jakarta.persistence.*;

/**
 * Test entity with explicit PUBLIC schema.
 * Used to verify that entities with schema annotations are correctly placed in their specified schema.
 */
@Entity
@Table(name = "first_table", schema = "PUBLIC")
public class FirstTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
