package com.example.ejb3.auction;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Setter
@Entity
public class User extends Persistent {
    @Getter
    private String userName;
    @Getter
    private String password;
    @Getter
    private String email;
    @Getter
    private Name name;
    private List<Bid> bids;
    private List<AuctionItem> auctions;

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL)
    public List<AuctionItem> getAuctions() {
        return auctions;
    }

    @OneToMany(mappedBy = "bidder", cascade = CascadeType.ALL)
    public List<Bid> getBids() {
        return bids;
    }

    public String toString() {
        return userName;
    }

}
