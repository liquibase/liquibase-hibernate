package com.example.multischema.auction;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;

@Audited
@Entity
@Table(schema = "PUBLIC")
public class AuditedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AUDITED_ITEM_SEQ")
    @SequenceGenerator(name = "AUDITED_ITEM_SEQ", sequenceName = "AUDITED_ITEM_SEQ")
    private long id;
    @Column(unique = true)
    private String name;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
