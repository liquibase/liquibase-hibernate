package com.example.ejb3.auction;

import java.util.Date;

import javax.annotation.Generated;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class AuctionInfo {
    private long id;
    private String description;
    private Date ends;
    private Float maxAmount;

    @Column(length = 1000)
    public String getDescription() {
        return description;
    }

    public Date getEnds() {
        return ends;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long getId() {
        return id;
    }

    public Float getMaxAmount() {
        return maxAmount;
    }

    public AuctionInfo(long id, String description, Date ends, Float maxAmount) {
        this.id = id;
        this.description = description;
        this.ends = ends;
        this.maxAmount = maxAmount;
    }

}
