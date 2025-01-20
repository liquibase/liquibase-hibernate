package com.example.ejb3.auction;

import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
public class AuctionItem extends Persistent {
    @Column(length = 1000)
    @Getter
    @Setter
    private String description;
    @Column(length = 200)
    @Getter
    @Setter
    private String shortDescription;
    @Setter
    private List<Bid> bids;
    @Setter
    private Bid successfulBid;
    @Setter
    private User seller;
    @Getter
    @Setter
    private Date ends;
    @Getter
    @Setter
    private int condition;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    public List<Bid> getBids() {
        return bids;
    }

    @ManyToOne
    public User getSeller() {
        return seller;
    }

    @ManyToOne
    public Bid getSuccessfulBid() {
        return successfulBid;
    }

    public String toString() {
        return shortDescription + " (" + description + ": " + condition
                + "/10)";
    }

}
