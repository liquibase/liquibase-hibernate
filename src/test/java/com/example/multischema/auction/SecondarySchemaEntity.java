package com.example.multischema.auction;

import jakarta.persistence.*;

/**
 * Test entity with explicit SECOND schema and sequence generator.
 * Used to verify that:
 * 1. Entities are correctly placed in non-default schemas
 * 2. Sequence generators with schema specifications are handled correctly
 */
@Entity
@Table(name = "secondary_schema_entity", schema = "SECOND")
@SequenceGenerator(name = "secondary_schema_entity_seq", schema = "SECOND", sequenceName = "secondary_schema_entity_seq")
public class SecondarySchemaEntity {

    @Id
    @GeneratedValue(generator = "secondary_schema_entity_seq")
    private Long id;

    private String email;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
