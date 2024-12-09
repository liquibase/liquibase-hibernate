package com.example.multischema.auction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(schema = "PUBLIC")
public class User extends Persistent {
    private String userName;
    private String password;
    private String email;
    private Name name;
    private List<Bid> bids;
    private List<AuctionItem> auctions;

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getUserName() {
        return userName;
    }

    public void setEmail(String string) {
        email = string;
    }

    public void setPassword(String string) {
        password = string;
    }

    public void setUserName(String string) {
        userName = string;
    }

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL)
    public List<AuctionItem> getAuctions() {
        return auctions;
    }

    @OneToMany(mappedBy = "bidder", cascade = CascadeType.ALL)
    public List<Bid> getBids() {
        return bids;
    }

    public void setAuctions(List<AuctionItem> list) {
        auctions = list;
    }

    public void setBids(List<Bid> list) {
        bids = list;
    }

    public String toString() {
        return userName;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

}
