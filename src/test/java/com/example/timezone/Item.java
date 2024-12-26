package com.example.timezone;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @Column
    private Instant timestamp1;

    @Column
    private LocalDateTime timestamp2;

    @Column(columnDefinition = "timestamp")
    private Instant timestamp3;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private LocalDateTime timestamp4;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Instant getTimestamp1() {
        return timestamp1;
    }

    public void setTimestamp1(Instant timestamp1) {
        this.timestamp1 = timestamp1;
    }

    public LocalDateTime getTimestamp2() {
        return timestamp2;
    }

    public void setTimestamp2(LocalDateTime timestamp2) {
        this.timestamp2 = timestamp2;
    }

    public Instant getTimestamp3() {
        return timestamp3;
    }

    public void setTimestamp3(Instant timestamp3) {
        this.timestamp3 = timestamp3;
    }

    public LocalDateTime getTimestamp4() {
        return timestamp4;
    }

    public void setTimestamp4(LocalDateTime timestamp4) {
        this.timestamp4 = timestamp4;
    }

}
