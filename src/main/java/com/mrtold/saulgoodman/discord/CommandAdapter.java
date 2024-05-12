package com.mrtold.saulgoodman.discord;

import com.mrtold.saulgoodman.Main;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.image.DocUtils;
import com.mrtold.saulgoodman.model.Advocate;
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
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * @author Mr_Told
 */
public class CommandAdapter extends ListenerAdapter {

    final DiscordUtils dsUtils;
    final DatabaseConnector db;
    final Logger log;
    final ZoneId timezone = ZoneId.of("Europe/Moscow");
    final DateTimeFormatter timestampFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss");

    public CommandAdapter(DiscordUtils dsUtils, DatabaseConnector db) {
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

        if (event.getChannelIdLong() != dsUtils.getAuditChannelId()) {
            event.reply(dsUtils.dict("cmd.err.no_perm")).setEphemeral(true).queue();
            return;
        }

        Member targetMember;
        Integer passport;
        String name;
        Message.Attachment signature;
        byte[] signatureB;
        Advocate advocate;
        Client client;
        TextChannel personalChannel;
        MessageCreateData mcd;

        boolean updateClientsRegistry = false;

        switch (event.getName().toLowerCase(Locale.ROOT)) {
            case Main.CMD_SIGN:
                advocate = db.getAdvocateByDiscord(event.getUser().getIdLong());
                if (advocate == null) {
                    log.error("Could not find advocate for user {}", event.getUser().getName());
                    event.reply(dsUtils.dict("cmd.err.no_perm")).setEphemeral(true).queue();
                    break;
                }

                event.deferReply().queue();
                targetMember = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.user"), OptionMapping::getAsMember));
                passport = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.pass"), OptionMapping::getAsInt));
                name = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.name"), OptionMapping::getAsString));
                signature = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.signature"), OptionMapping::getAsAttachment));
                int num = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.num"), OptionMapping::getAsInt));

                try {
                    signatureB = signature.getProxy().download().get().readAllBytes();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                byte[] agreement = DocUtils.generateAgreement(advocate.getName(), advocate.getPassport(),
                        advocate.getSignature(), name, passport, signatureB, num);

                client = db.getOrCreateClient(passport, targetMember.getIdLong(), name);
                personalChannel = createPersonalChannel(event.getGuild(), client.getDsUserChannel(),
                        name, targetMember.getIdLong(), advocate);
                db.updateClient(passport, targetMember.getIdLong(), name, personalChannel.getIdLong(), true);

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
                    event.reply(dsUtils.dict("cmd.err.no_perm"))
                            .setEphemeral(true).queue();
                    return;
                }
                event.deferReply().queue();

                targetMember = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.user"), OptionMapping::getAsMember));
                passport = event.getOption(dsUtils.dict("cmd.arg.pass"), OptionMapping::getAsInt);
                name = dsUtils.getMemberNick(targetMember);
                String reason = event.getOption(dsUtils.dict("cmd.arg.term_reason"),
                        dsUtils.dict("str.not_spec"), OptionMapping::getAsString);
                client = db.getClient(targetMember.getIdLong(), passport);
                if (client != null) {
                    db.updateClient(passport, targetMember.getIdLong(), client.getName(), client.getDsUserChannel(), false);
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
                                        targetMember.getAsMention(),
                                        dsUtils.getEmbedData(name),
                                        dsUtils.getEmbedData(passport),
                                        reason,
                                        event.getUser().getAsMention())).build());

                event.getHook().sendMessage(mcd).queue();

                if (client != null && client.getDsUserChannel() != -1) {
                    personalChannel = event.getGuild().getTextChannelById(client.getDsUserChannel());
                    if (personalChannel != null) personalChannel.sendMessage(mcd).queue();
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
                signature = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.signature"), OptionMapping::getAsAttachment));
                try (InputStream is = signature.getProxy().download().get()) {
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
            default:
                log.warn("Unknown command {} used by {}", event.getName(), event.getUser().getName());
                break;
        }

        if (updateClientsRegistry) {
            TextChannel registryChannel = event.getGuild().getTextChannelById(dsUtils.getClientRegistryChannelId());

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
                registry = registryChannel.sendMessage("Реестр активных клиентов адвокатского бюро:").complete();
            }

            StringBuilder sb = new StringBuilder("Имя, Фамилия - Паспорт - Тег - Канал\n");
            for (Client cl : db.getClientsByPass().values()) {
                if (!cl.isSigned()) continue;
                sb.append(cl.getName()).append(" - ").append(cl.getPassport()).append(" - ");
                Member m = event.getGuild().retrieveMemberById(cl.getDsUserId()).complete();
                if (m != null)
                        sb.append(m.getAsMention());
                else
                    sb.append("NO");
                sb.append(" - ");
                if (cl.getDsUserChannel() != -1) {
                    TextChannel c = event.getGuild().getTextChannelById(cl.getDsUserChannel());
                    if (c != null)
                        sb.append(c.getAsMention());
                    else sb.append("NO");
                } else
                    sb.append("NO");
                sb.append("\n");
            }
            registry.editMessage(MessageEditData.fromEmbeds(
                    prepareEmbedBuilder(15132410, dsUtils.dict("embed.title.registry"))
                            .setDescription(sb.toString())
                            .build()
            )).queue();
        }
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private TextChannel createPersonalChannel(Guild guild, long cId, String name, long dsUId, @Nullable Advocate advocate) {
        TextChannel channel = null;
        if (cId != -1) {
            channel = guild.getTextChannelById(cId);
        }

        if (channel == null) {
            channel = guild.createTextChannel(name,
                            guild.getCategoriesByName("клиенты", true).get(0))
                    .complete();
        }

        TextChannelManager permManager = channel.getManager();
        permManager.putMemberPermissionOverride(dsUId,
                Permission.getRaw(Permission.VIEW_CHANNEL), 0);
        if (advocate != null)
            permManager.putMemberPermissionOverride(advocate.getDsUserId(),
                    Permission.getRaw(Permission.VIEW_CHANNEL), 0);
        permManager.queue();
        return channel;
    }

}
