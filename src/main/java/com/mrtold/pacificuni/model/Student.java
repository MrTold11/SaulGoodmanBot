package com.mrtold.pacificuni.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Mr_Told
 */
@Entity
@Table(name = "Students")
public class Student {

    @Id
    long dsUserId;
    int passport;
    String name;
    @ElementCollection(fetch =  FetchType.EAGER)
    Set<Integer> courses = new HashSet<>();

    public Student(int passport, long dsUserId, String name) {
        this.passport = passport;
        this.dsUserId = dsUserId;
        this.name = name;
    }

    public Student() {}

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

    public Set<Integer> getCourses() {
        return courses;
    }

    public void setCourses(Set<Integer> courses) {
        if (courses == null)
            this.courses.clear();
        else
            this.courses = courses;
    }
}
