package com.mrtold.saulgoodman.logic.model;

import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

import static jakarta.persistence.GenerationType.SEQUENCE;


/**
 * @author Mr_Told
 */
@Entity
@Table(name = "receipt", indexes = {
        @Index(columnList = "author"),
        @Index(columnList = "client")
})
public class Receipt {

    @Id
    @SequenceGenerator(name = "receipt_seq", sequenceName = "receipt_seq", allocationSize = 1)
    @GeneratedValue(strategy = SEQUENCE, generator = "receipt_seq")
    int id;
    @Column(nullable = false)
    @NotNull
    Date issued;
    @Nullable
    Long ds_id;
    int status;
    int author;
    int client;
    int amount;

    public Receipt(int status, int author, int client, int amount, @Nullable Long dsId) {
        this.issued = new Date();
        this.ds_id = dsId;
        this.status = status;
        this.author = author;
        this.client = client;
        this.amount = amount;
    }

    public Receipt() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NotNull
    public Date getIssued() {
        return issued;
    }

    public void setIssued(@NotNull Date issued) {
        this.issued = issued;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getAuthor() {
        return author;
    }

    public void setAuthor(int author) {
        this.author = author;
    }

    public int getClient() {
        return client;
    }

    public void setClient(int client) {
        this.client = client;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Nullable
    public Long getDs_id() {
        return ds_id;
    }

    public void setDs_id(@Nullable Long ds_id) {
        this.ds_id = ds_id;
    }
}
