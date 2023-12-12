package com.mrtold.pacificuni.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Mr_Told
 */
public class Course {

    static Map<Integer, Course> courses = new HashMap<>();

    public static Course register(int id, String name, long roleId) {
        Course c = new Course(id, name, roleId);
        if (courses.containsKey(id))
            throw new IllegalArgumentException(String.format("Course with this ID already exists: %d", id));
        courses.put(id, c);
        return c;
    }

    public static Course getById(int id) {
        return courses.get(id);
    }

    final int id;
    final String name;
    long roleId;
    Faculty faculty;

    private Course(int id, String name, long roleId) {
        this.id = id;
        this.name = name;
        this.roleId = roleId;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getRoleId() {
        return roleId;
    }

    public void setRoleId(long roleId) {
        this.roleId = roleId;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }
}
