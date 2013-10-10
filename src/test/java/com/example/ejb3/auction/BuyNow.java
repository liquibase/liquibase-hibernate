package com.example.ejb3.auction;

import javax.persistence.Entity;

@Entity
public class BuyNow extends Bid {
    public boolean isBuyNow() {
        return true;
    }

    public String toString() {
        return super.toString() + " (buy now)";
    }
}
