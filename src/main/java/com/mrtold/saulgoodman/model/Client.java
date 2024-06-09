package com.mrtold.saulgoodman.model;

import jakarta.persistence.*;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

/**
 * @author Mr_Told
 */
@Entity
@Table(name = "client")
public class Client {

    @Id
    int passport;
    String name;
    @Nullable
    Long dsUserId;
    @Nullable
    Long dsUserChannel;

    public Client(int passport, @Nullable Long dsUserId, String name, @Nullable Long dsUserChannel) {
        this.passport = passport;
        this.dsUserId = dsUserId;
        this.name = name;
        this.dsUserChannel = dsUserChannel;
    }

    public Client() {}

    public int getPassport() {
        return passport;
    }

    public void setPassport(int passport) {
        this.passport = passport;
    }

    @Nullable
    public Long getDsUserId() {
        return dsUserId;
    }

    public void setDsUserId(long dsUserId) {
        this.dsUserId = dsUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    public Long getDsUserChannel() {
        return dsUserChannel;
    }

    public void setDsUserChannel(@Nullable Long dsUserChannel) {
        this.dsUserChannel = dsUserChannel;
    }

    public void setDsUserId(@Nullable Long dsUserId) {
        this.dsUserId = dsUserId;
    }

}
