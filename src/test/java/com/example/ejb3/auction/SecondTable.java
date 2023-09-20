package com.example.ejb3.auction;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SecondTable {

    @Column(table = "second_table")
    private String secondName;

    public String getSecondName() {
        return secondName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }
}
