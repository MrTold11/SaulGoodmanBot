package com.mrtold.saulgoodman.model;

import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * @author Mr_Told
 */
@Entity
@Table(name = "\"case\"")
public class Case {

    @Id
    @GeneratedValue
    int id;
    @Column(nullable = false)
    @NotNull
    String name;
    @Column(columnDefinition="TEXT", nullable=false)
    @NotNull
    String description;
    @Column(nullable = false)
    @NotNull
    Date opened_date;
    Date closed_date;

    public Case(int id, @NotNull String name, @NotNull String description, @NotNull Date opened_date, Date closed_date) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.opened_date = opened_date;
        this.closed_date = closed_date;
    }

    public Case() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    @NotNull
    public Date getOpened_date() {
        return opened_date;
    }

    public void setOpened_date(@NotNull Date opened_date) {
        this.opened_date = opened_date;
    }

    public Date getClosed_date() {
        return closed_date;
    }

    public void setClosed_date(Date closed_date) {
        this.closed_date = closed_date;
    }
}
