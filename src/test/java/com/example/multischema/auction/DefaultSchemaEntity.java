package com.example.multischema.auction;

import jakarta.persistence.*;

/**
 * Test entity without explicit schema declaration.
 * Used to verify that entities without schema annotations use the default schema behavior.
 */
@Entity
@Table(name = "default_schema_entity")
public class DefaultSchemaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}