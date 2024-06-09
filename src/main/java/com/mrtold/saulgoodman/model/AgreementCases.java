package com.mrtold.saulgoodman.model;

import jakarta.persistence.*;

/**
 * @author Mr_Told
 */
@Entity
@Table(name = "agreements_cases", indexes = {
        @Index(columnList = "\"case\""),
        @Index(columnList = "agreement")
})
public class AgreementCases {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    long id;
    @Column(name = "\"case\"")
    int case_;
    int agreement;

    public AgreementCases(int agreement, int case_) {
        this.agreement = agreement;
        this.case_ = case_;
    }

    public AgreementCases() {}

    public int getCase() {
        return case_;
    }

    public void setCase(int case_) {
        this.case_ = case_;
    }

    public int getAgreement() {
        return agreement;
    }

    public void setAgreement(int agreement) {
        this.agreement = agreement;
    }
}
