package com.example.ejb3.auction;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class Item {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator="ITEM_SEQ")
    @SequenceGenerator(name="ITEM_SEQ",sequenceName="ITEM_SEQ")
    private long id;
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
