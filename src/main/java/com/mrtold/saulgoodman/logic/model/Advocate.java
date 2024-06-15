package com.mrtold.saulgoodman.logic.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;

/**
 * @author Mr_Told
 */
@Entity
@Table(name = "advocate", indexes = @Index(columnList = "dsUserId"))
public class Advocate {

    @Id
    int passport;
    String name;
    long dsUserId;
    @Expose
    byte[] signature;
    int active;

    String passLink, licenseLink, signatureLink;
    Integer phone;

    public Advocate(int passport, long dsUserId, String name, byte[] signature) {
        this.passport = passport;
        this.dsUserId = dsUserId;
        this.name = name;
        this.signature = signature;
        active = 1;
    }

    public Advocate(int passport, String name, long dsUserId, byte[] signature,
                    String passLink, String licenseLink, String signatureLink, Integer phone) {
        this.passport = passport;
        this.name = name;
        this.dsUserId = dsUserId;
        this.signature = signature;
        this.passLink = passLink;
        this.licenseLink = licenseLink;
        this.signatureLink = signatureLink;
        this.phone = phone;
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

    public String getPassLink() {
        return passLink;
    }

    public void setPassLink(String passLink) {
        this.passLink = passLink;
    }

    public String getLicenseLink() {
        return licenseLink;
    }

    public void setLicenseLink(String licenseLink) {
        this.licenseLink = licenseLink;
    }

    public String getSignatureLink() {
        return signatureLink;
    }

    public void setSignatureLink(String signatureLink) {
        this.signatureLink = signatureLink;
    }

    public Integer getPhone() {
        return phone;
    }

    public void setPhone(Integer phone) {
        this.phone = phone;
    }

    public void setDsUserId(long dsUserId) {
        this.dsUserId = dsUserId;
    }

    public int getActive() {
        return active;
    }

    public void setActive(int active) {
        this.active = active;
    }

    public boolean isNotActive() {
        return active != 1;
    }

}
