package com.example.multischema.auction;

import jakarta.persistence.*;

@Entity
@Table // schema "PUBLIC" via default
public class Watcher {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "WATCHER_SEQ")
    @TableGenerator(name = "WATCHER_SEQ", table = "WatcherSeqTable")
    private Integer id;

    @SuppressWarnings("unused")
    private String name;

    @ManyToOne
    private AuctionItem auctionItem;
}