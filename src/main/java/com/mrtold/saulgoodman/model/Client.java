package com.mrtold.saulgoodman.model;

import jakarta.persistence.*;

/**
 * @author Mr_Told
 */
@Entity
@Table(name = "Clients")
public class Client {

    @Id
    long dsUserId;
    int passport;
    String name;
    long dsUserChannel;
    boolean signed;

    public Client(int passport, long dsUserId, String name, long dsUserChannel, boolean signed) {
        this.passport = passport;
        this.dsUserId = dsUserId;
        this.name = name;
        this.dsUserChannel = dsUserChannel;
        this.signed = signed;
    }

    public Client() {}

    public int getPassport() {
        return passport;
    }

    public void setPassport(int passport) {
        this.passport = passport;
    }

    public long getDsUserId() {
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

    public long getDsUserChannel() {
        return dsUserChannel;
    }

    public void setDsUserChannel(long dsUserChannel) {
        this.dsUserChannel = dsUserChannel;
    }

    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }
}
