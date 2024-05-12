package com.mrtold.saulgoodman.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Mr_Told
 */
@Entity
@Table(name = "Advocates")
public class Advocate {

    @Id
    long dsUserId;
    int passport;
    String name;
    byte[] signature;

    public Advocate(int passport, long dsUserId, String name, byte[] signature) {
        this.passport = passport;
        this.dsUserId = dsUserId;
        this.name = name;
        this.signature = signature;
    }

    public Advocate() {}

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

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }
}
