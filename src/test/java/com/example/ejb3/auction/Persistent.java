package com.example.ejb3.auction;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
public class Persistent {
    @Setter
    private Long id;

    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

}
