package com.example.springpackagescanning.auction;

import javax.persistence.Entity;

/**
 * @author Gavin King
 */
@Entity
public class BuyNow extends Bid {
    public boolean isBuyNow() {
        return true;
    }

    public String toString() {
        return super.toString() + " (buy now)";
    }
}
