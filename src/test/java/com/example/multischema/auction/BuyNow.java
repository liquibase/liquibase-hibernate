package com.example.multischema.auction;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(schema = "PUBLIC")
public class BuyNow extends Bid {

    @Transient
    public boolean isBuyNow() {
        return true;
    }

    public String toString() {
        return super.toString() + " (buy now)";
    }
}
