package com.mrtold.pacificuni.model;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Mr_Told
 */
public enum Faculty {

    BASIC("Дополнительное образование", 1182475179612438549L, "Центр Дополнительного образования",
            Course.register(101, "Основы жизни в San Andreas", 1183025324301234237L)
    ),
    LAW("Юридический", 1182446621393440768L, "Юридический факультет",
            Course.register(201, "Базовое законодательство San Andreas", 1183026403696971836L),
            Course.register(202, "Юриспруденция", 1183026555383980123L)
    ),
    JOURNALISM("Журналистика", 1182447649899696218L, "Факультет Журналистики"),
    BUSINESS("Бизнес и Управление", 1182448737256218794L, "Факультет Бизнеса и Управления"),
    ECONOMICS("Экономические и Социальные науки", 1182452023640203354L, "Факультет Экономических и Социальных наук");

    @NotNull
    final String name, alias, desc;

    @NotNull
    final Course[] courses;
    final long memberRoleId;

    Faculty(@NotNull String name, long memberRoleId, @NotNull String desc) {
        this.name = name;
        this.desc = desc;
        this.alias = name().toLowerCase(Locale.ROOT);
        this.memberRoleId = memberRoleId;
        this.courses = new Course[0];
    }

    Faculty(@NotNull String name, long memberRoleId, @NotNull String desc, @NotNull Course... courses) {
        this.name = name;
        this.desc = desc;
        this.alias = name().toLowerCase(Locale.ROOT);
        this.memberRoleId = memberRoleId;
        this.courses = courses;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getDesc() {
        return desc;
    }

    @NotNull
    public String getAlias() {
        return alias;
    }

    public long getMemberRoleId() {
        return memberRoleId;
    }

    @NotNull
    public Course[] getCourses() {
        return courses;
    }

    static final Map<String, Faculty> resolver = new HashMap<>();

    static {
        for (Faculty f : Faculty.values()) {
            resolver.put(f.name().toLowerCase(Locale.ROOT), f);
            for (Course course : f.getCourses())
                course.setFaculty(f);
        }
    }

    public static Faculty byAlias(String alias) {
        return resolver.get(alias);
    }

}
