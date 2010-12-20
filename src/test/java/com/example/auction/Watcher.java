package com.example.auction;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Watcher {

    @Id
    private Integer id;

    private String name;
    
    @ManyToOne
    private AuctionItem auctionItem;
}
