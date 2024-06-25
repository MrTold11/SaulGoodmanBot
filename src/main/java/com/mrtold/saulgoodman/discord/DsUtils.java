package com.mrtold.saulgoodman.discord;

import com.mrtold.saulgoodman.Config;
import com.mrtold.saulgoodman.Main;
import com.mrtold.saulgoodman.logic.model.Advocate;
import com.mrtold.saulgoodman.utils.Strings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Mr_Told
 */
public class DsUtils {

    private final static Guild guild;

    static {
        try {
            Main.getJDA().awaitReady();
            guild = Main.getJDA().getGuildById(Config.getInstance().getGuildId());
            if (guild == null)
                throw new IllegalStateException("Could not find guild");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize discord guild", e);
        }
    }

    private static final ZoneId timezone = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter timestampFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss");

    public static final Consumer<Message> MSG_DELETE_10 = m -> m.delete().queueAfter(10, TimeUnit.SECONDS);

    public static Guild getGuild() {
        return guild;
    }

    public static TextChannel getAuditChannel() {
        return guild.getTextChannelById(Config.getInstance().getAuditChannelId());
    }

    public static void addRoleToMember(Long dsId, long roleId) {
        if (dsId == null) return;
        try {
            guild.addRoleToMember(guild.getMemberById(dsId), guild.getRoleById(roleId)).queue();
        } catch (Exception ignored) {}
    }

    public static void removeRoleFromMember(Long dsId, long roleId) {
        if (dsId == null) return;
        try {
            guild.removeRoleFromMember(guild.getMemberById(dsId), guild.getRoleById(roleId)).queue();
        } catch (Exception ignored) {}
    }

    public static boolean hasNotClientPerms(@Nullable Member member) {
        return hasNotPerms(member, Config.getInstance().getClientRoleId());
    }

    public static boolean hasNotAdvocatePerms(@Nullable Long dsId) {
        if (dsId == null) return false;
        try {
            return hasNotAdvocatePerms(guild.retrieveMemberById(dsId).complete());
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean hasNotAdvocatePerms(@Nullable Member member) {
        return hasNotPerms(member, Config.getInstance().getAdvocateRoleId());
    }

    public static boolean hasNotHighPermission(@Nullable Long dsId) {
        if (dsId == null) return false;
        try {
            return hasNotHighPermission(guild.retrieveMemberById(dsId).complete());
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean hasNotHighPermission(@Nullable Member member) {
        return hasNotPerms(member, Config.getInstance().getHeadsRoleId());
    }

    private static boolean hasNotPerms(@Nullable Member member, long... roleIds) {
        if (member == null)
            return true;

        if (member.hasPermission(Permission.ADMINISTRATOR))
            return false;

        Set<Role> roles = Arrays.stream(roleIds)
                .mapToObj(member.getJDA()::getRoleById).collect(Collectors.toSet());

        return member.getRoles().stream().noneMatch(roles::contains);
    }

    public static @NotNull String getEmbedData(@Nullable Object o) {
        String str = o == null ? null : o.toString();
        return str == null || str.isBlank() ? Strings.getInstance().get("str.not_spec") : str;
    }

    public static @NotNull String getMemberAsMention(@Nullable Long dsId) {
        if (dsId == null) return Strings.getInstance().get("str.not_spec");
        return "<@" + dsId + ">";
    }

    public static @NotNull String getRoleAsMention(@Nullable Long roleId) {
        if (roleId == null) return Strings.getInstance().get("str.not_spec");
        if (roleId == guild.getPublicRole().getIdLong()) return "@everyone";
        return "<@&" + roleId + ">";
    }

    @NotNull
    public static EmbedBuilder prepareEmbedBuilder(int color, String title) {
        Strings s = Strings.getInstance();
        return new EmbedBuilder()
                .setAuthor(s.get("embed.author.name"), s.get("embed.author.url"), s.get("embed.author.icon"))
                .setTitle(title, s.get("embed.title.url"))
                .setColor(color)
                .setFooter(formatCurrentTime(), s.get("embed.footer.icon"));
    }

    public static String formatCurrentTime() {
        return timestampFormat.format(LocalDateTime.now(timezone));
    }

    public static void archivePersonalChannel(TextChannel channel) {
        if (channel == null) return;
        Config config = Config.getInstance();

        if (channel.getParentCategory() == null ||
                !channel.getParentCategory().getName().equalsIgnoreCase(config.getArchiveCategory())) {
            TextChannelManager permManager = channel.getManager();
            permManager.setParent(guild.getCategoriesByName(config.getArchiveCategory(), true).get(0)).queue();
        }
    }

    @Nullable
    public static TextChannel getChannelById(@Nullable Long channelId) {
        if (channelId == null) return null;
        return guild.getTextChannelById(channelId);
    }

    @NotNull
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static TextChannel createPersonalChannel(Long cId, String name, @Nullable Long dsUId,
                                                    @Nullable Advocate advocate, int clientPass, Integer agNum) {
        Config config = Config.getInstance();

        TextChannel channel = null;
        if (cId != null) {
            channel = guild.getTextChannelById(cId);
        }

        if (channel == null) {
            channel = guild.createTextChannel(name,
                            guild.getCategoriesByName(config.getClientsCategory(), true).get(0))
                    .complete();
        } else {
            channel.getManager().setName(name).queue();
        }

        channel.getManager().setTopic(Strings.getInstance().get("str.personal_channel_topic_pass_ag")
                .formatted(clientPass, getEmbedData(agNum))).queue();

        TextChannelManager permManager = channel.getManager();

        if (channel.getParentCategory() == null ||
                !channel.getParentCategory().getName().equalsIgnoreCase(config.getClientsCategory())) {
            permManager.setParent(guild.getCategoriesByName(config.getClientsCategory(), true).get(0));
        }

        if (dsUId != null)
            permManager.putMemberPermissionOverride(dsUId,
                    Permission.getRaw(Permission.VIEW_CHANNEL), 0);
        if (advocate != null)
            permManager.putMemberPermissionOverride(advocate.getDsUserId(),
                    Permission.getRaw(Permission.VIEW_CHANNEL), 0);
        permManager.queue();
        return channel;
    }

    public static Supplier<byte[]> attachmentSupplier(Message.Attachment attachment) {
        return () -> {
            try {
                return attachment.getProxy().download().get().readAllBytes();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static TextInput formTextInput(String id, String nameDescKey, TextInputStyle style,
                                          int minLen, int maxLen, boolean required) {
        Strings s = Strings.getInstance();
        return TextInput.create(id,
                        s.get("embed.modal." + nameDescKey + ".label"), style)
                .setPlaceholder(s.get("embed.modal." + nameDescKey + ".desc"))
                .setMinLength(minLen)
                .setMaxLength(maxLen)
                .setRequired(required)
                .build();
    }

}
