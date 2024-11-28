package com.example.multischema.auction;

import jakarta.persistence.*;

@Entity
@Table(schema = "SECOND")
@SequenceGenerator(name = "secondary_schema_entity_seq", schema = "SECOND")
public class SecondarySchemaEntity {

    private Long id;

    private String email;

    private Name name;


    @Id
    @GeneratedValue(generator = "secondary_schema_entity_seq")
    public Long getId() {
        return id;
    }

    public void setId(Long long1) {
        id = long1;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String string) {
        email = string;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

}
