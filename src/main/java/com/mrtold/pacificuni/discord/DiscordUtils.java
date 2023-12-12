package com.mrtold.pacificuni.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Mr_Told
 */
public class DiscordUtils {

    final JDA jda;

    long[] managerRoleIds;
    long[] teacherRoleIds;
    long studentRoleId, graduateRoleId;

    Map<String, String> dict = new HashMap<>();

    public DiscordUtils(JDA jda) {
        this.jda = jda;
    }

    public DiscordUtils addDict(String... strs) {
        if (strs.length % 2 != 0)
            throw new IllegalArgumentException("Strings must be paired (key - value)!");

        for (int i = 0; i < strs.length; i+=2) {
            if (dict.containsKey(strs[i]))
                throw new IllegalArgumentException(
                        String.format(Locale.getDefault(), "Dict key is already assigned: %s", strs[i]));

            dict.put(strs[i], strs[i + 1]);
        }

        return this;
    }

    public DiscordUtils setManagerRoleIds(long... ids) {
        managerRoleIds = ids;
        return this;
    }

    public DiscordUtils setTeacherRoleIds(long... ids) {
        teacherRoleIds = ids;
        return this;
    }

    public DiscordUtils setStudentRoleId(long id) {
        studentRoleId = id;
        return this;
    }

    public DiscordUtils setGraduateRoleId(long id) {
        graduateRoleId = id;
        return this;
    }

    public String dict(String key) {
        return dict.get(key);
    }

    public Role getStudentRole() {
        return jda.getRoleById(studentRoleId);
    }

    public Role getGraduateRole() {
        return jda.getRoleById(graduateRoleId);
    }

    public boolean hasManagerPermission(@Nullable Member member) {
        return hasPerms(member, managerRoleIds);
    }

    public boolean hasTeacherPermission(@Nullable Member member) {
        return hasPerms(member, teacherRoleIds);
    }

    private boolean hasPerms(@Nullable Member member, long[] roleIds) {
        if (member == null)
            return false;

        if (member.hasPermission(Permission.ADMINISTRATOR))
            return true;

        Set<Role> roles = Arrays.stream(roleIds)
                .mapToObj(jda::getRoleById).collect(Collectors.toSet());

        return member.getRoles().stream().anyMatch(roles::contains);
    }

    public @NotNull String getEmbedData(@Nullable Object o) {
        String str = o == null ? null : o.toString();
        return str == null || str.isBlank() ? dict("str.not_spec") : str;
    }

    public @Nullable String getMemberNick(@Nullable Member member) {
        return member == null || member.getNickname() == null ?
                null : member.getNickname();
    }

}
