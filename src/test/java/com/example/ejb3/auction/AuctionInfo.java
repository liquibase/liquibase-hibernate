package com.example.ejb3.auction;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class AuctionInfo {
    private String id;
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
    public String getId() {
        return id;
    }


    public Float getMaxAmount() {
        return maxAmount;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEnds(Date ends) {
        this.ends = ends;
    }

    public void setMaxAmount(Float maxAmount) {
        this.maxAmount = maxAmount;
    }

    public AuctionInfo(String id, String description, Date ends, Float maxAmount) {
        this.id = id;
        this.description = description;
        this.ends = ends;
        this.maxAmount = maxAmount;
    }

}
