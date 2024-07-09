package com.mrtold.saulgoodman.logic.model;

import jakarta.persistence.*;

import static jakarta.persistence.GenerationType.SEQUENCE;

/**
 * @author Mr_Told
 */
@Entity
@Table(name = "evidence")
public class Evidence {

    @Id
    @SequenceGenerator(name = "ev_seq", sequenceName = "ev_seq", allocationSize = 1)
    @GeneratedValue(strategy = SEQUENCE, generator = "ev_seq")
    long id;

    @Column(nullable = false)
    String name;
    @Column(nullable = false)
    String link;
    Integer obtaining;

    long claim;

    public Evidence() {}

    public Evidence(long id, String name, String link, Integer obtaining, long claim) {
        this.id = id;
        this.name = name;
        this.link = link;
        this.obtaining = obtaining;
        this.claim = claim;
    }

    public Evidence(String name, String link, Integer obtaining, long claim) {
        this.name = name;
        this.link = link;
        this.obtaining = obtaining;
        this.claim = claim;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Integer getObtaining() {
        return obtaining;
    }

    public void setObtaining(Integer obtaining) {
        this.obtaining = obtaining;
    }

    public long getClaim() {
        return claim;
    }

    public void setClaim(long claim) {
        this.claim = claim;
    }
}
