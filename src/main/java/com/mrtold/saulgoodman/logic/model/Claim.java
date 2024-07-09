package com.mrtold.saulgoodman.logic.model;

import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

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

    @Column(name = "forum")
    String forumLink;
    String header;
    @Column(name = "payment")
    String paymentLink;

    //todo connections
    //clients, lawyers, evidences

    //todo constructors, getters & setters

}
