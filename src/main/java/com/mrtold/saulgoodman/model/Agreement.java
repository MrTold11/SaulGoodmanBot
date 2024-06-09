package com.mrtold.saulgoodman.model;

import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * @author Mr_Told
 */
@Entity
@Table(name = "agreement", indexes = {
        @Index(columnList = "advocate"),
        @Index(columnList = "client")
})
public class Agreement {

    @Id
    int number;
    @Column(nullable = false)
    @NotNull
    Date signed_date;
    int status;
    int advocate;
    int client;

    public Agreement(int number, @NotNull Date signed_date, int status, int advocate, int client) {
        this.number = number;
        this.signed_date = signed_date;
        this.status = status;
        this.advocate = advocate;
        this.client = client;
    }

    public Agreement() {}

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @NotNull
    public Date getSigned_date() {
        return signed_date;
    }

    public void setSigned_date(@NotNull Date signed_date) {
        this.signed_date = signed_date;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getAdvocate() {
        return advocate;
    }

    public void setAdvocate(int advocate) {
        this.advocate = advocate;
    }

    public int getClient() {
        return client;
    }

    public void setClient(int client) {
        this.client = client;
    }
}
