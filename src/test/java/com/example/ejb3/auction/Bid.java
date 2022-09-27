package com.example.ejb3.auction;

import java.util.Date;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("Y")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
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

    @Transient
    public boolean isBuyNow() {
        return false;
    }

}
