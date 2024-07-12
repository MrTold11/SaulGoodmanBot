package com.mrtold.saulgoodman.logic.model;

import com.google.gson.annotations.JsonAdapter;
import com.mrtold.saulgoodman.api.AdvocateJsonAdapter;
import com.mrtold.saulgoodman.api.ClientJsonAdapter;
import com.mrtold.saulgoodman.api.EvidenceJsonAdapter;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Set;

import static jakarta.persistence.GenerationType.SEQUENCE;

/**
 * @author Mr_Told
 */
@Entity
@Table(name = "claim")
public class Claim {

    @Id
    @SequenceGenerator(name = "claim_seq", sequenceName = "claim_seq", allocationSize = 1)
    @GeneratedValue(strategy = SEQUENCE, generator = "claim_seq")
    long id;

    @Column(columnDefinition="TEXT")
    String description;
    String type;
    Integer number;
    int status, side;

    @Column(nullable = false)
    @NotNull
    Date happened;
    Date sent, hearing;

    String forumLink, paymentLink;
    String header;

    @JsonAdapter(ClientJsonAdapter.class)
    @ManyToMany(fetch = FetchType.EAGER)
    Set<Client> clients;

    @JsonAdapter(AdvocateJsonAdapter.class)
    @ManyToMany(fetch = FetchType.EAGER)
    Set<Advocate> advocates;

    @JsonAdapter(EvidenceJsonAdapter.class)
    @ManyToMany(fetch = FetchType.EAGER)
    Set<Evidence> evidences;

    public Claim() {
    }

    public Claim(String description, String type, Integer number, int status, @NotNull Date happened) {
        this.description = description;
        this.type = type;
        this.number = number;
        this.status = status;
        this.happened = happened;
    }

    public Evidence addEvidence(Evidence evidence) {
        evidences.add(evidence);
        evidence.setClaim(this);
        return evidence;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getSide() {
        return side;
    }

    public void setSide(int side) {
        this.side = side;
    }

    @NotNull
    public Date getHappened() {
        return happened;
    }

    public void setHappened(@NotNull Date happened) {
        this.happened = happened;
    }

    public Date getSent() {
        return sent;
    }

    public void setSent(Date sent) {
        this.sent = sent;
    }

    public Date getHearing() {
        return hearing;
    }

    public void setHearing(Date hearing) {
        this.hearing = hearing;
    }

    public String getForumLink() {
        return forumLink;
    }

    public void setForumLink(String forumLink) {
        this.forumLink = forumLink;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getPaymentLink() {
        return paymentLink;
    }

    public void setPaymentLink(String paymentLink) {
        this.paymentLink = paymentLink;
    }

    public Set<Client> getClients() {
        return clients;
    }

    public void setClients(Set<Client> clients) {
        this.clients = clients;
    }

    public Set<Advocate> getAdvocates() {
        return advocates;
    }

    public void setAdvocates(Set<Advocate> advocates) {
        this.advocates = advocates;
    }

    public Set<Evidence> getEvidences() {
        return evidences;
    }

    public void setEvidences(Set<Evidence> evidences) {
        this.evidences = evidences;
    }
}
