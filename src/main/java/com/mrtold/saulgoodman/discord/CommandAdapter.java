package com.mrtold.saulgoodman.discord;

import com.mrtold.saulgoodman.Main;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.image.DocUtils;
import com.mrtold.saulgoodman.imgur.ImgurUtils;
import com.mrtold.saulgoodman.model.Advocate;
import com.mrtold.saulgoodman.model.Agreement;
import com.mrtold.saulgoodman.model.Client;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * @author Mr_Told
 */
public class CommandAdapter extends ListenerAdapter {

    final ImgurUtils imgurUtils;
    final DiscordUtils dsUtils;
    final DatabaseConnector db;
    final Logger log;
    final ZoneId timezone = ZoneId.of("Europe/Moscow");
    final DateTimeFormatter timestampFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss");

    public CommandAdapter(ImgurUtils imgurUtils, DiscordUtils dsUtils, DatabaseConnector db) {
        this.imgurUtils = imgurUtils;
        this.dsUtils = dsUtils;
        this.db = db;
        this.log = LoggerFactory.getLogger(CommandAdapter.class);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply(dsUtils.dict("cmd.err.no_guild")).setEphemeral(true).queue();
            return;
        }

        if (!dsUtils.hasAdvocatePerms(event.getMember())) {
            event.reply(dsUtils.dict("cmd.err.no_perm")).setEphemeral(true).queue();
            return;
        }

        if (!event.getName().equalsIgnoreCase(Main.CMD_NAME) && event.getChannelIdLong() != dsUtils.getAuditChannelId()) {
            event.reply(dsUtils.dict("cmd.err.no_perm")).setEphemeral(true).queue();
            return;
        }

        Member targetMember;
        Integer passport;
        String name;
        Message.Attachment attachment;
        byte[] signatureB;
        Advocate advocate;
        Client client;
        TextChannel personalChannel;
        MessageCreateData mcd;

        boolean updateClientsRegistry = false;

        switch (event.getName().toLowerCase(Locale.ROOT)) {
            case Main.CMD_SIGN:
                advocate = db.getAdvocateByDiscord(event.getUser().getIdLong());
                if (advocate == null || advocate.getSignature() == null) {
                    log.error("Could not find advocate for user {}", event.getUser().getName());
                    event.reply(dsUtils.dict("cmd.err.no_perm")).setEphemeral(true).queue();
                    break;
                }

                targetMember = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.user"), OptionMapping::getAsMember));
                passport = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.pass"), OptionMapping::getAsInt));
                name = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.name"), OptionMapping::getAsString));
                attachment = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.signature"), OptionMapping::getAsAttachment));
                int num = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.num"), OptionMapping::getAsInt));

                if (db.clientHasActiveAgreement(passport)) {
                    event.reply(dsUtils.dict("cmd.err.already_has_agreement")).setEphemeral(true).queue();
                    break;
                }
                event.deferReply().queue();

                try {
                    signatureB = attachment.getProxy().download().get().readAllBytes();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                byte[] agreement = DocUtils.generateAgreement(advocate.getName(), advocate.getPassport(),
                        advocate.getSignature(), name, passport, signatureB, num);

                client = db.getOrCreateClient(passport, targetMember.getIdLong(), name);
                personalChannel = createPersonalChannel(event.getGuild(), client.getDsUserChannel(),
                        name, targetMember.getIdLong(), advocate);
                db.updateClient(client, targetMember.getIdLong(), name, personalChannel.getIdLong());
                db.saveAgreement(new Agreement(num, new Date(), 1, advocate.getPassport(), passport));

                mcd = MessageCreateData.fromEmbeds(
                        prepareEmbedBuilder(8453888, dsUtils.dict("embed.title.sign"))
                                .setDescription(String.format(Locale.getDefault(),
                                                        """
                                                        Тег клиента: %s
                                                        Имя клиента: %s
                                                        Номер паспорта: %d
                                                        Адвокат: %s
                                                        """,
                                        targetMember.getAsMention(),
                                        dsUtils.getEmbedData(name),
                                        passport,
                                        event.getUser().getAsMention())).build());

                FileUpload[] files = new FileUpload[]{FileUpload.fromData(new ByteArrayInputStream(agreement),
                        "jma" + num + ".jpg")};

                event.getHook().sendMessage(mcd).addFiles(files).queue();
                personalChannel.sendMessage(mcd).addFiles(files).queue();

                guild.addRoleToMember(targetMember, guild.getRoleById(dsUtils.getClientRoleId())).queue();
                updateClientsRegistry = true;
                break;

            case Main.CMD_TERMINATE:
                if (!dsUtils.hasHighPermission(event.getMember())) {
                    event.reply(dsUtils.dict("cmd.err.no_perm")).setEphemeral(true).queue();
                    return;
                }

                targetMember = event.getOption(dsUtils.dict("cmd.arg.user"), OptionMapping::getAsMember);
                passport = event.getOption(dsUtils.dict("cmd.arg.pass"), OptionMapping::getAsInt);
                String reason = event.getOption(dsUtils.dict("cmd.arg.term_reason"),
                        dsUtils.dict("str.not_spec"), OptionMapping::getAsString);
                client = db.getClient(targetMember == null ? null : targetMember.getIdLong(), passport);

                if (client == null) {
                    event.reply(dsUtils.dict("cmd.err.client_nf")).setEphemeral(true).queue();
                    return;
                }

                Agreement a = db.getActiveAgreement(client.getPassport());
                if (a == null) {
                    event.reply(dsUtils.dict("cmd.err.client_nf")).setEphemeral(true).queue();
                    return;
                }
                event.deferReply().queue();
                a.setStatus(0);
                db.saveAgreement(a);

                passport = client.getPassport();
                name = client.getName();
                if (targetMember == null && client.getDsUserId() != null) {
                    targetMember = event.getGuild().retrieveMemberById(client.getDsUserId()).complete();
                }

                mcd = MessageCreateData.fromEmbeds(
                        prepareEmbedBuilder(14357564, dsUtils.dict("embed.title.terminate"))
                                .setDescription(String.format(Locale.getDefault(),
                                                """
                                                Тег клиента: %s
                                                Имя пользователя: %s
                                                Номер паспорта: %s
                                                Причина: **%s**
                                                Автор: %s
                                                """,
                                        targetMember == null ? "@" + client.getDsUserId() : targetMember.getAsMention(),
                                        dsUtils.getEmbedData(name),
                                        dsUtils.getEmbedData(passport),
                                        reason,
                                        event.getUser().getAsMention())).build());

                event.getHook().sendMessage(mcd).queue();

                if (client.getDsUserChannel() != null) {
                    personalChannel = event.getGuild().getTextChannelById(client.getDsUserChannel());
                    if (personalChannel != null) {
                        personalChannel.sendMessage(mcd).queue();
                        archivePersonalChannel(event.getGuild(), personalChannel);
                    }
                }

                guild.removeRoleFromMember(targetMember, guild.getRoleById(dsUtils.getClientRoleId())).queue();
                updateClientsRegistry = true;
                break;

            case Main.CMD_INVITE:
                if (!dsUtils.hasHighPermission(event.getMember())) {
                    event.reply(dsUtils.dict("cmd.err.no_perm"))
                            .setEphemeral(true).queue();
                    return;
                }

                event.deferReply().queue();
                targetMember = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.user"), OptionMapping::getAsMember));
                passport = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.pass"), OptionMapping::getAsInt));
                name = event.getOption(dsUtils.dict("cmd.arg.name"), OptionMapping::getAsString);
                attachment = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.signature"), OptionMapping::getAsAttachment));
                try (InputStream is = attachment.getProxy().download().get()) {
                    signatureB = is.readAllBytes();
                } catch (IOException | InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }

                advocate = db.getAdvocateByDiscord(targetMember.getIdLong());
                if (advocate == null) {
                    advocate = new Advocate(passport, targetMember.getIdLong(), name, signatureB);
                } else {
                    advocate.setName(name);
                    advocate.setSignature(signatureB);
                    advocate.setPassport(passport);
                }
                db.saveAdvocate(advocate);

                //generate system agreement with system user
                Agreement a1 = db.getAgreementById(100000 + passport);
                if (a1 == null)
                    a1 = new Agreement(100000 + passport, new Date(), 1, passport, 0);
                else
                    a1.setStatus(1);
                db.saveAgreement(a1);

                event.getHook().sendMessage(MessageCreateData.fromEmbeds(
                        prepareEmbedBuilder(15132410, dsUtils.dict("embed.title.invite"))
                                .setDescription(String.format(Locale.getDefault(),
                                                """
                                                Тег адвоката: %s
                                                Имя адвоката: %s
                                                Номер паспорта: %s
                                                Автор: %s
                                                """,
                                        targetMember.getAsMention(),
                                        dsUtils.getEmbedData(name),
                                        dsUtils.getEmbedData(passport),
                                        event.getUser().getAsMention())
                                )
                                .build()
                )).queue();

                guild.addRoleToMember(targetMember, guild.getRoleById(dsUtils.getAdvocateRoleId())).queue();
                break;

            case Main.CMD_NAME:
                passport = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.pass"), OptionMapping::getAsInt));
                name = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.name"), OptionMapping::getAsString));

                client = db.getClientByPass(passport);
                if (client == null) {
                    event.reply(dsUtils.dict("cmd.err.client_nf")).setEphemeral(true).queue();
                    return;
                }

                if (client.getName().equals(name)) {
                    event.reply(dsUtils.dict("cmd.err.client_name_ok")).setEphemeral(true).queue();
                    return;
                }
                event.deferReply().queue();

                client.setName(name);
                db.saveClient(client);

                if (client.getDsUserChannel() != null) {
                    personalChannel = event.getGuild().getTextChannelById(client.getDsUserChannel());
                    if (personalChannel != null) {
                        personalChannel.getManager().setName(name).queue();
                    }
                }

                event.getHook().sendMessage(dsUtils.dict("str.data_upd_ok")).setEphemeral(true).queue();
                break;
            case Main.CMD_ATTACH:
                advocate = db.getAdvocateByDiscord(event.getUser().getIdLong());
                if (advocate == null) {
                    event.reply(dsUtils.dict("cmd.err.no_perm"))
                            .setEphemeral(true).queue();
                    return;
                }

                event.deferReply().queue();

                String subCmd = Objects.requireNonNull(event.getSubcommandName());

                if (subCmd.equalsIgnoreCase(Main.CMD_ATTACH_PHONE)) {
                    int phone = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.num"), OptionMapping::getAsInt));
                    advocate.setPhone(phone);
                } else if (subCmd.equalsIgnoreCase(Main.CMD_ATTACH_NAME)) {
                    name = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.name"), OptionMapping::getAsString));
                    advocate.setName(name);
                } else {
                    attachment = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.doc_img"), OptionMapping::getAsAttachment));

                    if (subCmd.equalsIgnoreCase(Main.CMD_ATTACH_SIGNATURE)) {
                        try (InputStream is = attachment.getProxy().download().get()) {
                            signatureB = is.readAllBytes();
                        } catch (IOException | InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                        advocate.setSignature(signatureB);
                    } else {
                        //todo
                        break;
                    }
                }
                db.saveAdvocate(advocate);

                event.getHook().sendMessage(dsUtils.dict("str.data_upd_ok")).setEphemeral(true).queue();
                break;
            default:
                log.warn("Unknown command {} used by {}", event.getName(), event.getUser().getName());
                break;
        }

        if (updateClientsRegistry) updateClientsRegistry(event.getGuild());
    }

    private void updateClientsRegistry(Guild guild) {
        TextChannel registryChannel = guild.getTextChannelById(dsUtils.getClientRegistryChannelId());

        if (registryChannel == null) {
            log.error("Could not find channel for clients registry w/ id {}",
                    dsUtils.getClientRegistryChannelId());
            return;
        }

        Message registry = null;
        for (Message m : registryChannel.getHistory().retrievePast(5).complete()) {
            if (m.getAuthor().getIdLong() == dsUtils.jda.getSelfUser().getIdLong()) {
                registry = m;
                break;
            }
        }

        if (registry == null) {
            registry = registryChannel.sendMessage(MessageCreateData.fromEmbeds(
                    prepareEmbedBuilder(15132410, dsUtils.dict("embed.title.registry"))
                            .build())).complete();
        }

        StringBuilder sb = new StringBuilder("Соглашение - Имя, Фамилия - Паспорт - Тег - Канал\n");
        Map<Integer, Client> clientMap = new HashMap<>();
        db.getAllClients().forEach(client -> clientMap.put(client.getPassport(), client));

        for (Agreement agreement : db.getActiveAgreements()) {
            Client c = clientMap.get(agreement.getClient());
            if (c == null || c.getPassport() == 0) continue;
            sb
                    .append(agreement.getNumber()).append(" - ")
                    .append(c.getName()).append(" - ")
                    .append(c.getPassport()).append(" - ");

            try {
                if (c.getDsUserId() == null) throw new RuntimeException();
                Member m = guild.retrieveMemberById(c.getDsUserId()).complete();
                sb.append(m.getAsMention());
            } catch (Exception e) {
                sb.append("NO");
            }
            sb.append(" - ");

            TextChannel tc;
            if (c.getDsUserChannel() != null
                    && (tc = guild.getTextChannelById(c.getDsUserChannel())) != null)
                sb.append(tc.getAsMention());
            else sb.append("NO");
            sb.append("\n");
        }

        registry.editMessage(MessageEditData.fromEmbeds(
                prepareEmbedBuilder(15132410, dsUtils.dict("embed.title.registry"))
                        .setDescription(sb.toString())
                        .build()
        )).queue();
    }

    private EmbedBuilder prepareEmbedBuilder(int color, String title) {
        return new EmbedBuilder()
                .setAuthor(dsUtils.dict("embed.author.name"),
                        dsUtils.dict("embed.author.url"),
                        dsUtils.dict("embed.author.icon"))
                .setTitle(title, dsUtils.dict("embed.title.url"))
                .setColor(color)
                .setFooter(timestampFormat.format(LocalDateTime.now(timezone)),
                        dsUtils.dict("embed.footer.icon"));
    }

    private void archivePersonalChannel(Guild guild, TextChannel channel) {
        if (channel == null) return;

        if (channel.getParentCategory() == null ||
                !channel.getParentCategory().getName().equalsIgnoreCase("архив")) {
            TextChannelManager permManager = channel.getManager();
            permManager.setParent(guild.getCategoriesByName("архив", true).get(0)).queue();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private TextChannel createPersonalChannel(Guild guild, Long cId, String name, long dsUId,
                                              @Nullable Advocate advocate) {
        TextChannel channel = null;
        if (cId != null) {
            channel = guild.getTextChannelById(cId);
        }

        if (channel == null) {
            channel = guild.createTextChannel(name,
                            guild.getCategoriesByName("клиенты", true).get(0))
                    .complete();
        } else {
            channel.getManager().setName(name).queue();
        }

        TextChannelManager permManager = channel.getManager();

        if (channel.getParentCategory() == null ||
                !channel.getParentCategory().getName().equalsIgnoreCase("клиенты")) {
            permManager.setParent(guild.getCategoriesByName("клиенты", true).get(0));
        }

        permManager.putMemberPermissionOverride(dsUId,
                Permission.getRaw(Permission.VIEW_CHANNEL), 0);
        if (advocate != null)
            permManager.putMemberPermissionOverride(advocate.getDsUserId(),
                    Permission.getRaw(Permission.VIEW_CHANNEL), 0);
        permManager.queue();
        return channel;
    }

}
