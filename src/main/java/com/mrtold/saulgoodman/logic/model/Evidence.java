package com.mrtold.saulgoodman.logic.model;

import com.google.gson.annotations.JsonAdapter;
import com.mrtold.saulgoodman.api.json.AdvocateJsonAdapter;
import com.mrtold.saulgoodman.api.json.ClaimJsonAdapter;
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
    String obtaining;

    @JsonAdapter(ClaimJsonAdapter.class)
    @ManyToOne(fetch = FetchType.EAGER)
    Claim claim;

    @JsonAdapter(AdvocateJsonAdapter.class)
    @ManyToOne(fetch = FetchType.EAGER)
    Advocate author;

    public Evidence() {}

    public Evidence(long id, String name, String link, String obtaining, Claim claim, Advocate author) {
        this.id = id;
        this.name = name;
        this.link = link;
        this.obtaining = obtaining;
        this.claim = claim;
        this.author = author;
    }

    public Evidence(String name, String link, String obtaining, Claim claim, Advocate author) {
        this.name = name;
        this.link = link;
        this.obtaining = obtaining;
        this.claim = claim;
        this.author = author;
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

    public String getObtaining() {
        return obtaining;
    }

    public void setObtaining(String obtaining) {
        this.obtaining = obtaining;
    }

    public Claim getClaim() {
        return claim;
    }

    public void setClaim(Claim claim) {
        this.claim = claim;
    }

    public Advocate getAuthor() {
        return author;
    }

    public void setAuthor(Advocate author) {
        this.author = author;
    }
}
