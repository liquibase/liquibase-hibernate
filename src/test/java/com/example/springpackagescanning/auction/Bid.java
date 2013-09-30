package com.example.springpackagescanning.auction;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;

/**
 * @author Gavin King
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("Y")
public class Bid extends Persistent {
    private AuctionItem item;
    private float amount;
    private Date datetime;
    private User bidder;

    @ManyToOne
    public AuctionItem getItem() {
        return item;
    }

    public void setItem(AuctionItem item) {
        this.item = item;
    }

    public float getAmount() {
        return amount;
    }

    @Column(nullable = false, name = "datetime")
    public Date getDatetime() {
        return datetime;
    }

    public void setAmount(float f) {
        amount = f;
    }

    public void setDatetime(Date date) {
        datetime = date;
    }

    @ManyToOne(optional = false)
    public User getBidder() {
        return bidder;
    }

    public void setBidder(User user) {
        bidder = user;
    }

    public String toString() {
        return bidder.getUserName() + " $" + amount;
    }

    public boolean isBuyNow() {
        return false;
    }

}
