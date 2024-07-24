package com.mrtold.saulgoodman.logic.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;

import java.util.Date;

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
    @Column(columnDefinition = "int default 0")
    int role; // 0 - advocate, 1 - head

    @Expose
    byte[] signature;

    String passLink, licenseLink, signatureLink, profilePicture;
    Integer phone;

    Date joined, resigned;

    public Advocate(int passport, long dsUserId, String name, byte[] signature) {
        this.passport = passport;
        this.dsUserId = dsUserId;
        this.name = name;
        this.signature = signature;
        this.joined = new Date();
    }

    public Advocate(int passport, String name) {
        this.passport = passport;
        this.name = name;
    }

    public Advocate(int passport, String name, long dsUserId, int role, byte[] signature,
                    String passLink, String licenseLink, String signatureLink, String profilePicture,
                    Integer phone, Date joined, Date resigned) {
        this.passport = passport;
        this.name = name;
        this.dsUserId = dsUserId;
        this.role = role;
        this.signature = signature;
        this.passLink = passLink;
        this.licenseLink = licenseLink;
        this.signatureLink = signatureLink;
        this.profilePicture = profilePicture;
        this.phone = phone;
        this.joined = joined;
        this.resigned = resigned;
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

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public Date getJoined() {
        return joined;
    }

    public void setJoined(Date joined) {
        this.joined = joined;
    }

    public Date getResigned() {
        return resigned;
    }

    public void setResigned(Date resigned) {
        this.resigned = resigned;
    }

    public boolean isNotActive() {
        return resigned != null;
    }

}
