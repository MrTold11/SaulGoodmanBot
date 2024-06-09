package com.mrtold.saulgoodman.model;

import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

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
    @GeneratedValue(strategy=GenerationType.AUTO)
    int id;
    @Column(nullable = false)
    @NotNull
    Date issued;
    int status;
    int author;
    int client;
    int amount;

    public Receipt(int id, @NotNull Date issued, int status, int author, int client, int amount) {
        this.id = id;
        this.issued = issued;
        this.status = status;
        this.author = author;
        this.client = client;
        this.amount = amount;
    }

    public Receipt() {

    }

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
}
