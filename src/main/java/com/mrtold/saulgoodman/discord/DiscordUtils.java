package com.mrtold.saulgoodman.discord;

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
    long clientRoleId, advocateRoleId, headsRoleId;
    long clientRegistryChannelId, auditChannelId;

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

    public DiscordUtils setClientRoleId(long id) {
        clientRoleId = id;
        return this;
    }

    public DiscordUtils setAdvocateRoleId(long id) {
        advocateRoleId = id;
        return this;
    }

    public DiscordUtils setHeadsRoleId(long id) {
        headsRoleId = id;
        return this;
    }

    public DiscordUtils setClientRegistryChannelId(long id) {
        clientRegistryChannelId = id;
        return this;
    }

    public DiscordUtils setAuditChannelId(long id) {
        auditChannelId = id;
        return this;
    }

    public String dict(String key) {
        return dict.get(key);
    }

    public long getClientRoleId() {
        return clientRoleId;
    }

    public long getAdvocateRoleId() {
        return advocateRoleId;
    }

    public long getHeadsRoleId() {
        return headsRoleId;
    }

    public long getClientRegistryChannelId() {
        return clientRegistryChannelId;
    }

    public long getAuditChannelId() {
        return auditChannelId;
    }

    public boolean hasClientPerms(@Nullable Member member) {
        return hasPerms(member, clientRoleId);
    }

    public boolean hasAdvocatePerms(@Nullable Member member) {
        return hasPerms(member, advocateRoleId);
    }

    public boolean hasHighPermission(@Nullable Member member) {
        return hasPerms(member, headsRoleId);
    }

    private boolean hasPerms(@Nullable Member member, long... roleIds) {
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
