package com.mrtold.saulgoodman.logic.model;

import jakarta.persistence.*;
import org.jetbrains.annotations.Nullable;

/**
 * @author Mr_Told
 */
@Entity
@Table(name = "client")
public class Client {

    @Id
    int passport;
    String name;

    long dsUserId;
    //todo replace with cases channels
    @Nullable
    Long dsUserChannel;

    Integer agreement;

    Integer phone;
    String passportLink, agreementLink;

    public Client(Integer passport, String name) {
        this.passport = passport;
        this.name = name;
    }

    public Client(int passport, long dsUserId, String name, @Nullable Long dsUserChannel) {
        this.passport = passport;
        this.dsUserId = dsUserId;
        this.name = name;
        this.dsUserChannel = dsUserChannel;
    }

    public Client(int passport, String name, long dsUserId, Integer phone) {
        this.passport = passport;
        this.name = name;
        this.dsUserId = dsUserId;
        this.phone = phone;
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

    @Nullable
    public Long getDsUserChannel() {
        return dsUserChannel;
    }

    public void setDsUserChannel(@Nullable Long dsUserChannel) {
        this.dsUserChannel = dsUserChannel;
    }

    public Integer getAgreement() {
        return agreement;
    }

    public void setAgreement(Integer agreement) {
        this.agreement = agreement;
    }

    public Integer getPhone() {
        return phone;
    }

    public void setPhone(Integer phone) {
        this.phone = phone;
    }

    public String getPassportLink() {
        return passportLink;
    }

    public void setPassportLink(String passportLink) {
        this.passportLink = passportLink;
    }

    public String getAgreementLink() {
        return agreementLink;
    }

    public void setAgreementLink(String agreementLink) {
        this.agreementLink = agreementLink;
    }
}
