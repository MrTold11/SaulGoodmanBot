package com.mrtold.saulgoodman.discord;

import com.mrtold.saulgoodman.Main;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.image.DocUtils;
import com.mrtold.saulgoodman.imgur.ImgurUtils;
import com.mrtold.saulgoodman.model.Advocate;
import com.mrtold.saulgoodman.model.Agreement;
import com.mrtold.saulgoodman.model.Client;
import com.mrtold.saulgoodman.model.Receipt;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
    final DateFormat df = new SimpleDateFormat("dd.MM.yyyy");

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

        boolean updateClientsRegistry = false, updateReceiptRegistry = false;

        switch (event.getName().toLowerCase(Locale.ROOT)) {
            case Main.CMD_SIGN:
                advocate = advocateSearch(event.getUser(), event);
                if (advocate == null) break;

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
                openBill(event, targetMember, client, advocate, Main.DEFAULT_AGREEMENT_BILL_AMOUNT);
                updateClientsRegistry = true;
                updateReceiptRegistry = true;
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
                event.deferReply(true).queue();
                passport = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.pass"), OptionMapping::getAsInt));
                name = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.name"), OptionMapping::getAsString));

                client = db.getClientByPass(passport);
                if (client == null) {
                    event.getHook().sendMessage(dsUtils.dict("cmd.err.client_nf")).queue();
                    return;
                }

                if (client.getName().equals(name)) {
                    event.getHook().sendMessage(dsUtils.dict("cmd.err.client_name_ok")).queue();
                    return;
                }

                client.setName(name);
                db.saveClient(client);

                if (client.getDsUserChannel() != null) {
                    personalChannel = event.getGuild().getTextChannelById(client.getDsUserChannel());
                    if (personalChannel != null) {
                        personalChannel.getManager().setName(name).queue();
                    }
                }

                updateClientsRegistry = true;
                event.getHook().sendMessage(dsUtils.dict("str.data_upd_ok")).queue();
                break;
            case Main.CMD_ATTACH:
                event.deferReply(true).queue();
                advocate = db.getAdvocateByDiscord(event.getUser().getIdLong());
                if (advocate == null) {
                    event.getHook().sendMessage(dsUtils.dict("cmd.err.no_perm")).queue();
                    return;
                }

                String subCmd = Objects.requireNonNull(event.getSubcommandName());

                if (subCmd.equalsIgnoreCase(Main.CMD_ATTACH_PHONE)) {
                    int phone = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.phone"), OptionMapping::getAsInt));
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

                event.getHook().sendMessage(dsUtils.dict("str.data_upd_ok")).queue();
                break;
            case Main.CMD_RECEIPT:
                advocate = advocateSearch(event.getUser(), event);
                if (advocate == null) break;

                targetMember = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.user"), OptionMapping::getAsMember));
                int amount = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.amount"), OptionMapping::getAsInt));
                passport = event.getOption(dsUtils.dict("cmd.arg.pass"), OptionMapping::getAsInt);

                if (amount < 1) {
                    event.reply(dsUtils.dict("cmd.err.amount_low")).setEphemeral(true).queue();
                    break;
                }

                client = db.getClient(targetMember.getIdLong(), passport);
                if (client == null) {
                    event.reply(dsUtils.dict("cmd.err.client_nf")).setEphemeral(true).queue();
                    break;
                }
                event.deferReply().queue();
                openBill(event, targetMember, client, advocate, amount);
                updateReceiptRegistry = true;
                break;
            default:
                log.warn("Unknown command {} used by {}", event.getName(), event.getUser().getName());
                break;
        }

        if (updateClientsRegistry) updateClientsRegistry(event.getGuild());
        if (updateReceiptRegistry) updateReceiptRegistry(event.getGuild());
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getGuild() == null) return;
        if (event.getComponentId().equals("bill_payed")) {
            if (!dsUtils.hasHighPermission(event.getMember())) {
                event.reply(dsUtils.dict("cmd.err.no_perm"))
                        .setEphemeral(true).queue();
                return;
            }

            event.deferReply(true).queue();
            Receipt receipt;
            try {
                receipt = db.getReceipt(Integer.parseInt(Objects.requireNonNull(event.getMessage().getEmbeds().get(0)
                        .getDescription()).split("Номер счета: #")[1].split("\n")[0]));
            } catch (Exception e) {
                receipt = null;
            }

            if (receipt == null) {
                event.getHook().sendMessage(dsUtils.dict("cmd.err.receipt_nf")).queue();
                return;
            }

            receipt.setStatus(1);
            db.saveReceipt(receipt);
            event.getMessage().editMessage("Помечено как оплачено " +
                    Objects.requireNonNull(event.getMember()).getAsMention() + " от " +
                    timestampFormat.format(LocalDateTime.now(timezone))).setComponents().queue();
            event.getHook().sendMessage(String.format(dsUtils.dict("str.receipt_paid"), receipt.getId())).queue();
        } else if (event.getComponentId().equals("agreement_request")) {
            TextInput name = TextInput.create("agreement_request_name", "Имя, Фамилия", TextInputStyle.SHORT)
                    .setPlaceholder("Имя Фамилия (( IC ))")
                    .setMinLength(5)
                    .setMaxLength(50)
                    .build();
            TextInput pass = TextInput.create("agreement_request_pass", "Паспорт", TextInputStyle.SHORT)
                    .setPlaceholder("Паспорт (( IC ))")
                    .setMinLength(1)
                    .setMaxLength(8)
                    .build();
            TextInput desc = TextInput.create("agreement_request_desc", "Описание", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Если вам необходима юридическая помощь, кратко опишите," +
                            " что произошло, и что бы вы хотели сделать")
                    .setRequired(false)
                    .build();

            Modal modal = Modal.create("agreement_request_form", "Заявка")
                    .addComponents(ActionRow.of(name), ActionRow.of(pass), ActionRow.of(desc))
                    .build();
            event.replyModal(modal).queue();
        } else if (event.getComponentId().startsWith("aReq_")) {
            if (!dsUtils.hasAdvocatePerms(event.getMember())) {
                event.reply(dsUtils.dict("cmd.err.no_perm"))
                        .setEphemeral(true).queue();
                return;
            }

            Advocate advocate = advocateSearch(event.getUser(), event);
            if (advocate == null) return;

            event.deferReply(true).queue();
            int pass = Integer.parseInt(event.getComponentId().split("_")[2]);
            Client client = db.getClientByPass(pass);
            if (client == null || client.getDsUserChannel() == null || client.getDsUserId() == null) {
                event.getHook().sendMessage(dsUtils.dict("cmd.err.client_nf")).queue();
                return;
            }

            TextChannel tc = event.getGuild().getTextChannelById(client.getDsUserChannel());
            if (tc == null) {
                db.deleteClient(client);
                event.getMessage().editMessage("Канал клиента был удален, заявка отклонена.")
                        .setComponents().queue();
                event.getHook().sendMessage("Канал клиента был удален, заявка отменена.").queue();
                return;
            }

            if (event.getComponentId().startsWith("aReq_acc_")) {
                Objects.requireNonNull(tc).getManager()
                        .putMemberPermissionOverride(advocate.getDsUserId(),
                                Permission.getRaw(Permission.VIEW_CHANNEL), 0)
                        .putMemberPermissionOverride(client.getDsUserId(),
                                Permission.getRaw(Permission.VIEW_CHANNEL), 0)
                        .queue();
                event.getMessage().editMessage("Принято адвокатом: " +
                        Objects.requireNonNull(event.getMember()).getAsMention()).setComponents().queue();
                event.getHook().sendMessage("Вы приняли запрос, доступ к каналу клиента открыт!").queue();
            } else {
                db.deleteClient(client);
                tc.delete().queue();
                event.getMessage().editMessage("Отклонено адвокатом: " +
                        Objects.requireNonNull(event.getMember()).getAsMention()).setComponents().queue();
                event.getHook().sendMessage("Вы отклонили запрос, клиент удален!").queue();
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getGuild() == null) return;
        if (event.getModalId().equals("agreement_request_form")) {
            event.deferReply(true).queue();
            String name = extractModalValue(event, "agreement_request_name");
            String pass = extractModalValue(event, "agreement_request_pass");
            String desc = extractModalValue(event, "agreement_request_desc");
            int passport;
            long dsId = Objects.requireNonNull(event.getMember()).getIdLong();

            try {
                if (pass == null) throw new RuntimeException();
                passport = Integer.parseInt(pass);
                if (passport < 1) throw new RuntimeException();
            } catch (Exception e) {
                event.getHook().sendMessage("Введен некорректный номер паспорта. Подайте заявку заново.")
                        .queue();
                return;
            }

            Client client = db.getClientByPass(passport);
            if (client != null || name == null) {
                event.getHook().sendMessage("Произошла ошибка. Свяжитесь с руководством.")
                        .queue();
                return;
            }

            TextChannel tc = createPersonalChannel(event.getGuild(), null,
                    "❕・" + name, dsId, null);
            client = new Client(passport, dsId, name, tc.getIdLong());
            db.saveClient(client);

            Objects.requireNonNull(event.getGuild().getTextChannelById(dsUtils.getRequestsChannelId()))
                    .sendMessage(MessageCreateData.fromEmbeds(
                            prepareEmbedBuilder(15132410, dsUtils.dict("embed.title.agreement_request"))
                                    .setDescription(String.format(Locale.getDefault(),
                                            """
                                            Тег клиента: %s
                                            Имя клиента: %s
                                            Номер паспорта: %d
                                            Личный канал: %s
                                            Описание заявки: %s
                                            """,
                                            event.getMember().getAsMention(),
                                            dsUtils.getEmbedData(name),
                                            passport,
                                            tc.getAsMention(),
                                            dsUtils.getEmbedData(desc))).build()))
                    .setActionRow(Button.success("aReq_acc_" + passport, "Принять в работу"),
                            Button.danger("aReq_dec_" + passport, "Отказать"))
                    .queue();

            event.getHook().sendMessage("Заявка принята в обработку!" +
                    " Подробности в Вашем персональном канале ниже.").queue();
            tc.sendMessage(event.getMember().getAsMention() + """
                    \n**Добро пожаловать в Адвокатское бюро MBA Legal Group!**
                    Для заключения соглашения, потребуются некоторые игровые документы:
                    1. Скриншот паспорта
                    2. Номер телефона *(в игре)*
                    3. Подпись персонажа *(создать ее можно онлайн, в поисковике введите: "создать подпись онлайн")*
                    ||Обратите внимание: подпись должна быть черная или синяя, на белом или прозрачном фоне!||
                    
                    После отправки всех документов, ожидайте ответа свободного адвоката для заключения соглашения.
                    
                    Стоимость заключения соглашения составляет 10.000$. После заключения соглашения, оплату можно передать лично адвокату, либо перевести на банковский счет бюро.
                    """).queue();
            tc.sendMessage(String.format("Описание заявки клиента:\n%s", desc)).queue();
        }
    }

    private @Nullable String extractModalValue(@NotNull ModalInteractionEvent event, String id) {
        ModalMapping m = event.getValue(id);
        if (m == null) return null;
        return m.getAsString();
    }

    private Advocate advocateSearch(@NotNull User user, IReplyCallback event) {
        Advocate advocate = db.getAdvocateByDiscord(user.getIdLong());
        if (advocate == null || advocate.getSignature() == null) {
            log.error("Could not find advocate for user {}", user.getName());
            event.reply(dsUtils.dict("cmd.err.no_perm")).setEphemeral(true).queue();
        }
        return advocate;
    }

    private void openBill(SlashCommandInteractionEvent event, Member clientMember, @NotNull Client client,
                          Advocate advocate, int amount) {
        TextChannel personalChannel = null;
        if (client.getDsUserChannel() != null)
            personalChannel = Objects.requireNonNull(event.getGuild()).getTextChannelById(client.getDsUserChannel());

        Receipt r = db.saveReceipt(new Receipt(0, advocate.getPassport(), client.getPassport(), amount));

        MessageCreateData mcd = MessageCreateData.fromEmbeds(
                prepareEmbedBuilder(15132410, dsUtils.dict("embed.title.receipt"))
                        .setDescription(String.format(Locale.getDefault(),
                                """
                                Номер счета: #%d
                                Тег клиента: %s
                                Имя клиента: %s
                                Номер паспорта: %d
                                Адвокат: %s
                                Сумма к оплате: %d$
                                """,
                                r.getId(),
                                clientMember.getAsMention(),
                                dsUtils.getEmbedData(client.getName()),
                                client.getPassport(),
                                event.getUser().getAsMention(),
                                amount)).build());

        event.getHook().sendMessage(mcd).setActionRow(
                Button.success("bill_payed", dsUtils.dict("embed.button.payed")))
                .queue();
        if (personalChannel != null)
            personalChannel.sendMessage(MessageCreateBuilder.from(mcd)
                    .setContent(clientMember.getAsMention()).build()).queue();
    }

    private void updateClientsRegistry(Guild guild) {
        updateRegistry(guild, dsUtils.dict("embed.title.registry"),
                "Соглашение - Имя, Фамилия - Паспорт - Тег - Канал", db.getActiveAgreements(),
                (agreement, clientMap, sb) -> {
                    Client c = clientMap.get(agreement.getClient());
                    if (c == null || c.getPassport() == 0) return;
                    sb
                            .append(agreement.getNumber()).append(" - ")
                            .append(c.getName()).append(" - ")
                            .append(c.getPassport()).append(" - ");

                    registryAppendDs(guild, sb, c);
                    sb.append("\n");
                });
    }

    private void updateReceiptRegistry(Guild guild) {
        updateRegistry(guild, dsUtils.dict("embed.title.receipt_registry"),
                "Номер - Имя, Фамилия - Паспорт - Тег - Канал - Сумма - Дата", db.getActiveReceipts(),
                (receipt, clientMap, sb) -> {
                    Client c = clientMap.get(receipt.getClient());
                    if (c == null || c.getPassport() == 0) return;
                    sb
                            .append(receipt.getId()).append(" - ")
                            .append(c.getName()).append(" - ")
                            .append(c.getPassport()).append(" - ");

                    registryAppendDs(guild, sb, c);
                    sb.append(" - ").append(receipt.getAmount())
                            .append("$ - ").append(df.format(receipt.getIssued()));
                    sb.append("\n");
                });
    }

    private void registryAppendDs(Guild guild, StringBuilder sb, Client c) {
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
    }

    private <T> void updateRegistry(@NotNull Guild guild, String title, String tableTitle,
                                    List<T> items, RegistryFunc<T> func) {
        TextChannel registryChannel = guild.getTextChannelById(dsUtils.getClientRegistryChannelId());
        if (registryChannel == null) {
            log.error("Could not find channel for clients registry w/ id {}",
                    dsUtils.getClientRegistryChannelId());
            return;
        }

        Message registry = null;
        for (Message m : registryChannel.getHistory().retrievePast(5).complete()) {
            if (m.getAuthor().getIdLong() == dsUtils.jda.getSelfUser().getIdLong() &&
                    Objects.equals(m.getEmbeds().get(0).getTitle(), title)) {
                registry = m;
                break;
            }
        }

        if (registry == null) {
            registry = registryChannel.sendMessage(MessageCreateData.fromEmbeds(
                    prepareEmbedBuilder(15132410, title)
                            .build())).complete();
        }

        StringBuilder sb = new StringBuilder(tableTitle).append("\n");
        Map<Integer, Client> clientMap = new HashMap<>();
        db.getAllClients().forEach(client -> clientMap.put(client.getPassport(), client));

        for (T t : items)
            func.process(t, clientMap, sb);

        registry.editMessage(MessageEditData.fromEmbeds(
                prepareEmbedBuilder(15132410, title)
                        .setDescription(sb.toString())
                        .build()
        )).queue();
    }

    @NotNull
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

    @NotNull
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
