package com.example.multischema.auction;

import jakarta.persistence.*;

import java.util.Date;
import java.util.List;

@Entity
@Table(schema = "PUBLIC")
public class AuctionItem extends Persistent {
    private String description;
    private String shortDescription;
    private List<Bid> bids;
    private Bid successfulBid;
    private User seller;
    private Date ends;
    private int condition;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    public List<Bid> getBids() {
        return bids;
    }

    @Column(length = 1000)
    public String getDescription() {
        return description;
    }

    @ManyToOne
    public User getSeller() {
        return seller;
    }

    @ManyToOne
    public Bid getSuccessfulBid() {
        return successfulBid;
    }

    public void setBids(List<Bid> bids) {
        this.bids = bids;
    }

    public void setDescription(String string) {
        description = string;
    }

    public void setSeller(User user) {
        seller = user;
    }

    public void setSuccessfulBid(Bid bid) {
        successfulBid = bid;
    }

    public Date getEnds() {
        return ends;
    }

    public void setEnds(Date date) {
        ends = date;
    }

    public int getCondition() {
        return condition;
    }

    public void setCondition(int i) {
        condition = i;
    }

    public String toString() {
        return shortDescription + " (" + description + ": " + condition
                + "/10)";
    }

    @Column(length = 200)
    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }


}
