package com.example.springpackagescanning.auction;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * @author Gavin King
 */
@MappedSuperclass
public class Persistent {
    private Long id;

    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long long1) {
        id = long1;
    }

}
