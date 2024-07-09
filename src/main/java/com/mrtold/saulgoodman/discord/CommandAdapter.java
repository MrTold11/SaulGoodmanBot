package com.mrtold.saulgoodman.discord;

import com.mrtold.saulgoodman.Config;
import com.mrtold.saulgoodman.Main;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.logic.endpoint.*;
import com.mrtold.saulgoodman.logic.endpoint.attach.AttachName;
import com.mrtold.saulgoodman.logic.endpoint.attach.AttachPhone;
import com.mrtold.saulgoodman.logic.endpoint.attach.AttachSignature;
import com.mrtold.saulgoodman.logic.firstaid.FirstAidManager;
import com.mrtold.saulgoodman.logic.model.Advocate;
import com.mrtold.saulgoodman.logic.model.Agreement;
import com.mrtold.saulgoodman.logic.model.Client;
import com.mrtold.saulgoodman.utils.DocUtils;
import com.mrtold.saulgoodman.utils.Strings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
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
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.mrtold.saulgoodman.discord.DsUtils.MSG_DELETE_10;
import static com.mrtold.saulgoodman.discord.DsUtils.MSG_DELETE_60;

/**
 * @author Mr_Told
 */
public class CommandAdapter extends ListenerAdapter {

    final Strings s = Strings.getInstance();
    final Config config = Config.getInstance();
    final DatabaseConnector db = DatabaseConnector.getInstance();
    final Logger log = LoggerFactory.getLogger(CommandAdapter.class);

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        boolean isRequest = event.getName().equalsIgnoreCase(Main.CMD_REQUEST);
        if (!isRequest)
            event.deferReply(true).queue();

        Guild guild = event.getGuild();
        if (guild == null) {
            if (isRequest) event.deferReply(true).queue();
            event.getHook().sendMessage(s.get("cmd.err.no_guild")).queue(MSG_DELETE_10);
            return;
        }

        if (DsUtils.hasNotAdvocatePerms(event.getMember())) {
            if (isRequest) event.deferReply(true).queue();
            event.getHook().sendMessage(s.get("cmd.err.no_perm")).queue(MSG_DELETE_10);
            return;
        }

        Consumer<String> failureConsumer = s ->
                event.getHook().sendMessage(s).queue(MSG_DELETE_10);
        Function<String, Runnable> successFunc = s1 -> () ->
                event.getHook().sendMessage(s.get(s1)).queue(MSG_DELETE_10);

        long advocateId = event.getUser().getIdLong();
        Member targetMember = event.getOption(s.get("cmd.arg.user"), OptionMapping::getAsMember);
        Long clientDsId = targetMember == null ? null : targetMember.getIdLong();
        Integer passport = event.getOption(s.get("cmd.arg.pass"), OptionMapping::getAsInt);
        String name = event.getOption(s.get("cmd.arg.name"), OptionMapping::getAsString);
        String reason;
        final Message.Attachment attachment = event.getOption(s.get("cmd.arg.signature"), OptionMapping::getAsAttachment);

        switch (event.getName().toLowerCase(Locale.ROOT)) {
            case Main.CMD_SIGN:
                checkNotNull(clientDsId, passport, attachment, name);
                int num = Objects.requireNonNull(event.getOption(s.get("cmd.arg.num"), OptionMapping::getAsInt));
                //noinspection DataFlowIssue
                new SignAgreement(advocateId, clientDsId, name, passport, num,
                        DsUtils.attachmentSupplier(attachment))
                        .exec(successFunc.apply("str.cmd_success"), failureConsumer);
                break;

            case Main.CMD_TERMINATE:
                reason = event.getOption(s.get("cmd.arg.term_reason"), s.get("str.not_spec"), OptionMapping::getAsString);

                new TerminateAgreement(advocateId, clientDsId, passport, reason)
                        .exec(successFunc.apply("str.cmd_success"), failureConsumer);
                break;

            case Main.CMD_INVITE:
                checkNotNull(clientDsId, passport, attachment);
                //noinspection DataFlowIssue
                new InviteAdvocate(advocateId, clientDsId, passport, name,
                        DsUtils.attachmentSupplier(attachment))
                        .exec(successFunc.apply("str.cmd_success"), failureConsumer);
                break;

            case Main.CMD_UNINVITE:
                reason = event.getOption(s.get("cmd.arg.term_reason"), s.get("str.not_spec"), OptionMapping::getAsString);
                new UninviteAdvocate(advocateId, clientDsId, passport, reason)
                        .exec(successFunc.apply("str.cmd_success"), failureConsumer);
                break;

            case Main.CMD_NAME:
                checkNotNull(passport, name);
                //noinspection DataFlowIssue
                new RenameClient(advocateId, passport, name)
                        .exec(successFunc.apply("str.data_upd_ok"), failureConsumer);
                break;

            case Main.CMD_ATTACH:
                String subCmd = Objects.requireNonNull(event.getSubcommandName());

                if (subCmd.equalsIgnoreCase(Main.CMD_ATTACH_PHONE)) {
                    int phone = Objects.requireNonNull(event.getOption(s.get("cmd.arg.phone"), OptionMapping::getAsInt));
                    new AttachPhone(advocateId, phone)
                            .exec(successFunc.apply("str.data_upd_ok"), failureConsumer);
                } else if (subCmd.equalsIgnoreCase(Main.CMD_ATTACH_NAME)) {
                    checkNotNull(name);
                    new AttachName(advocateId, name)
                            .exec(successFunc.apply("str.data_upd_ok"), failureConsumer);
                } else {
                    Message.Attachment doc = Objects.requireNonNull(event.getOption(s.get("cmd.arg.doc_img"), OptionMapping::getAsAttachment));
                    if (subCmd.equalsIgnoreCase(Main.CMD_ATTACH_SIGNATURE)) {
                        new AttachSignature(advocateId, DsUtils.attachmentSupplier(doc))
                                .exec(successFunc.apply("str.data_upd_ok"), failureConsumer);
                    }
                    //todo
                }
                break;

            case Main.CMD_RECEIPT:
                checkNotNull(clientDsId);
                int amount = Objects.requireNonNull(event.getOption(s.get("cmd.arg.amount"), OptionMapping::getAsInt));

                new OpenBill(clientDsId, advocateId, passport, amount)
                        .exec(successFunc.apply("str.cmd_success"), failureConsumer);
                break;

            case Main.CMD_REMOVE:
                checkNotNull(passport);
                //noinspection DataFlowIssue
                new RemoveClient(advocateId, passport)
                        .exec(successFunc.apply("str.client_deleted"), failureConsumer);
                break;

            case Main.CMD_REQUEST:
                Advocate advocate = db.getAdvocateByDiscord(advocateId);
                if (advocate == null) {
                    logAdvocateNf(event.getUser().getName());
                    event.reply(s.get("cmd.err.no_perm")).setEphemeral(true).queue();
                    return;
                }

                Client client = db.getClientByChannel(event.getChannelIdLong());
                if (client == null) {
                    event.reply(s.get("cmd.err.client_nf")).setEphemeral(true).queue();
                    return;
                }

                Agreement agreement = db.getActiveAgreement(client.getPassport());
                if (agreement == null || agreement.getStatus() != 1) {
                    event.reply(s.get("cmd.err.agreement_nf")).setEphemeral(true).queue();
                    return;
                }

                TextInput reqFormNum = DsUtils.formTextInput("request_num", "num",
                        TextInputStyle.SHORT, 1, 5, true);
                TextInput reqFormBody = DsUtils.formTextInput("request_body", "body",
                        TextInputStyle.PARAGRAPH, 10, 4000, true);
                TextInput reqFormTarget = DsUtils.formTextInput("request_target", "target",
                        TextInputStyle.SHORT, 10, 500, true);
                TextInput reqFormDeadline = DsUtils.formTextInput("request_deadline", "deadline",
                        TextInputStyle.SHORT, 10, 100, true);

                Modal modal = Modal.create("request_form_%d".formatted(agreement.getNumber()), s.get("embed.modal.request"))
                        .addComponents(
                                ActionRow.of(reqFormNum), ActionRow.of(reqFormBody),
                                ActionRow.of(reqFormTarget), ActionRow.of(reqFormDeadline))
                        .build();
                event.replyModal(modal).queue();
                break;

            default:
                event.getHook().sendMessage(s.get("cmd.err.incorrect_cmd")).queue(MSG_DELETE_10);
                log.warn("Unknown command {} used by {}", event.getName(), event.getUser().getName());
                break;
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getGuild() == null || event.getGuild().getIdLong() != Config.getInstance().getGuildId())
            return;
        String buttonId = event.getComponentId();
        if (buttonId.startsWith("bill_payed_")) {
            event.deferReply(true).queue();

            new CloseBill(event.getUser().getIdLong(), event.getMessage(), buttonId.split("_")[2])
                    .exec(
                            () -> event.getHook().sendMessage(s.get("str.receipt_paid")).queue(MSG_DELETE_10),
                            s -> event.getHook().sendMessage(s).queue(MSG_DELETE_10));
        } else if (buttonId.equals("agreement_request")) {
            Client client = db.getClientByDiscord(event.getUser().getIdLong());
            if (client != null) {
                event.deferReply(true).queue();
                event.getHook().sendMessage(s.get("message.already_client")).queue(MSG_DELETE_10);
                return;
            }

            TextInput name = DsUtils.formTextInput("agreement_request_name", "name",
                    TextInputStyle.SHORT, 5, 50, true);
            TextInput pass = DsUtils.formTextInput("agreement_request_pass", "passport",
                    TextInputStyle.SHORT, 1, 8, true);
            TextInput desc = DsUtils.formTextInput("agreement_request_desc", "description",
                    TextInputStyle.PARAGRAPH, 0, 1500, false);

            Modal modal = Modal.create("agreement_request_form", s.get("embed.modal.ag_request"))
                    .addComponents(ActionRow.of(name), ActionRow.of(pass), ActionRow.of(desc))
                    .build();
            event.replyModal(modal).queue();

        } else if (buttonId.equals("firstaid_request")) {
            event.deferReply(true).queue();
            Client client = db.getClientByDiscord(event.getUser().getIdLong());
            if (client == null) {
                List<Client> twinks = db.getClientTwinks(event.getUser().getIdLong());
                twinks.removeIf(c -> !db.clientHasActiveAgreement(c.getPassport()));
                if (twinks.isEmpty()) {
                    event.getHook().sendMessage(s.get("cmd.err.no_perm")).queue(MSG_DELETE_10);
                    return;
                }

                if (twinks.size() == 1)
                    client = twinks.get(0);
                else {
                    event.getHook().sendMessage(s.get("message.select_twink"))
                            .setActionRow(twinks.stream().map(
                                    c -> Button.primary("twink_faReq_%d".formatted(c.getPassport()), c.getName())
                            ).toList()).queue(MSG_DELETE_60);
                    return;
                }
            } else if (!db.clientHasActiveAgreement(client.getPassport())) {
                event.getHook().sendMessage(s.get("cmd.err.no_perm")).queue(MSG_DELETE_10);
                return;
            }

            if (FirstAidManager.getInstance().registerRequest(client))
                event.getHook().sendMessage(s.get("message.firstaid_accepted")).queue(MSG_DELETE_10);
            else
                event.getHook().sendMessage(s.get("message.firstaid_timeout")).queue(MSG_DELETE_10);

        } else if (buttonId.startsWith("twink_faReq_")) {
            event.deferReply(true).queue();
            int pass = Integer.parseInt(buttonId.split("_")[2]);
            Client client = db.getClientByPass(pass);
            if (client == null || !Objects.equals(client.getDsUserId(), event.getUser().getIdLong())) {
                event.getHook().sendMessage(s.get("cmd.err.no_perm")).queue(MSG_DELETE_10);
                return;
            }

            if (FirstAidManager.getInstance().registerRequest(client))
                event.getHook().sendMessage(s.get("message.firstaid_accepted")).queue(MSG_DELETE_10);
            else
                event.getHook().sendMessage(s.get("message.firstaid_timeout")).queue(MSG_DELETE_10);

        } else if (buttonId.startsWith("faReq_")) {
            event.deferReply(true).queue();
            long id = Long.parseLong(buttonId.split("_")[1]);
            Advocate advocate = advocateSearch(event);
            if (advocate == null) return;

            event.getMessage().editMessage(s.get("str.request_accepted_by") +
                    DsUtils.getMemberAsMention(event.getUser().getIdLong())).setComponents().queue();
            FirstAidManager.getInstance().acceptRequest(id, advocate,
                    s -> event.getHook().sendMessage(s).queue(MSG_DELETE_10));

        } else if (buttonId.equals("firstaid_enter")) {
            event.deferReply(true).queue();
            Advocate advocate = advocateSearch(event);
            if (advocate == null) return;
            FirstAidManager.getInstance().startShift(advocate);
            event.getHook().sendMessage(s.get("message.duty_enter")).queue(MSG_DELETE_10);
        } else if (buttonId.equals("firstaid_exit")) {
            event.deferReply(true).queue();
            Advocate advocate = advocateSearch(event);
            if (advocate == null) return;
            FirstAidManager.getInstance().endShift(advocate);
            event.getHook().sendMessage(s.get("message.duty_exit")).queue(MSG_DELETE_10);
        } else if (buttonId.startsWith("aReq_")) {
            event.deferReply(true).queue();
            Advocate advocate = advocateSearch(event);
            if (advocate == null) return;

            int pass = Integer.parseInt(buttonId.split("_")[2]);
            Client client = db.getClientByPass(pass);
            if (client == null || client.getDsUserChannel() == null) {
                event.getHook().sendMessage(s.get("cmd.err.client_nf")).queue(MSG_DELETE_10);
                return;
            }

            TextChannel tc = event.getGuild().getTextChannelById(client.getDsUserChannel());
            if (tc == null) {
                db.deleteClient(client);
                event.getMessage().editMessage(s.get("str.request_cancelled"))
                        .setComponents().queue();
                event.getHook().sendMessage(s.get("str.request_cancelled")).queue(MSG_DELETE_10);
                return;
            }

            if (buttonId.startsWith("aReq_acc_")) {
                tc.upsertPermissionOverride(event.getMember()).grant(Permission.VIEW_CHANNEL).queue();
                event.getMessage().editMessage(s.get("str.request_accepted_by") +
                        Objects.requireNonNull(event.getMember()).getAsMention()).setComponents().queue();
                event.getHook().sendMessage(s.get("str.request_accepted")).queue(MSG_DELETE_10);
            } else {
                db.deleteClient(client);
                tc.delete().queue();
                event.getMessage().editMessage(s.get("str.request_declined_by") +
                        Objects.requireNonNull(event.getMember()).getAsMention()).setComponents().queue();
                event.getHook().sendMessage(s.get("str.request_declined")).queue(MSG_DELETE_10);
            }
        } else if (buttonId.startsWith("agr_")) {
            String[] split = buttonId.split("_");
            boolean ok = split[1].equals("ok");
            long advocateId = Long.parseLong(split[2]);
            int num = Integer.parseInt(split[3]);

            Advocate advocate = db.getAdvocateByDiscord(advocateId);
            if (advocate == null) {
                logAdvocateNf(event.getUser().getName());
                event.reply(s.get("cmd.err.no_perm")).setEphemeral(true).queue();
                return;
            }

            if ((event.getUser().getIdLong() != advocateId && DsUtils.hasNotHighPermission(event.getMember()))
                    || DsUtils.hasNotAdvocatePerms(event.getMember())) {
                event.reply(s.get("cmd.err.no_perm")).setEphemeral(true).queue();
                return;
            }

            Agreement a = db.getAgreementById(num);

            if (a == null) {
                event.getMessage().editMessageComponents(Collections.emptyList()).queue();
                event.reply(s.get("cmd.err.agreement_nf")).setEphemeral(true).queue();
                return;
            }

            if (ok) {
                TextInput agreementLink = DsUtils.formTextInput("agreement_link", "link",
                        TextInputStyle.SHORT, 10, 150, true);

                Modal modal = Modal.create("agreement_link_%d".formatted(a.getNumber()), s.get("embed.modal.ag_link"))
                        .addComponents(ActionRow.of(agreementLink)).build();
                event.replyModal(modal).queue();
            } else {
                event.deferReply(true).queue();
                event.getMessage().editMessageComponents(Collections.emptyList()).queue();
                log.info("Agreement {} has been cancelled by advocate {}", num, event.getUser().getIdLong());
                db.deleteAgreement(a);
                event.getMessage().editMessage(s.get("str.rolled_back")).setFiles(Collections.emptyList()).queue();
                event.getHook().sendMessage(s.get("str.agreement_cancelled")).queue(MSG_DELETE_10);
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getGuild() == null || event.getGuild().getIdLong() != Config.getInstance().getGuildId())
            return;
        if (event.getModalId().equals("agreement_request_form")) {
            event.deferReply(true).queue();
            String name = extractModalValue(event, "agreement_request_name");
            String pass = extractModalValue(event, "agreement_request_pass");
            String desc = extractModalValue(event, "agreement_request_desc");
            int passport;
            long dsId = event.getUser().getIdLong();

            try {
                if (pass == null) throw new RuntimeException();
                passport = Integer.parseInt(pass);
                if (passport < 1) throw new RuntimeException();
            } catch (Exception e) {
                event.getHook().sendMessage(s.get("message.request_wrong_pass"))
                        .queue(MSG_DELETE_10);
                return;
            }

            Client client = db.getClientByPass(passport);
            if (client != null || name == null || name.isBlank()) {
                event.getHook().sendMessage(s.get("message.request_failed"))
                        .queue(MSG_DELETE_10);
                return;
            }

            TextChannel tc = DsUtils.createPersonalChannel( null, "❕・" + name, dsId,
                    null, passport, null);
            client = new Client(passport, dsId, name, tc.getIdLong());

            Objects.requireNonNull(event.getGuild().getTextChannelById(config.getRequestsChannelId()))
                    .sendMessage(MessageCreateData.fromEmbeds(
                            DsUtils.prepareEmbedBuilder(15132410, s.get("embed.title.agreement_request"))
                                    .setDescription(String.format(Locale.getDefault(),
                                            s.get("embed.body.agreement_request"),
                                            DsUtils.getMemberAsMention(dsId),
                                            name,
                                            passport,
                                            tc.getAsMention(),
                                            DsUtils.getEmbedData(desc))).build()))
                    .setContent(DsUtils.getRoleAsMention(config.getAdvocateRoleId()))
                    .setActionRow(
                            Button.success("aReq_acc_" + passport, s.get("embed.button.request_accept")),
                            Button.danger("aReq_dec_" + passport, s.get("embed.button.request_decline")))
                    .queue();

            db.saveClient(client);
            event.getHook().sendMessage(s.get("message.request_accepted")).queue(MSG_DELETE_10);
            tc.sendMessage(DsUtils.getMemberAsMention(dsId) +
                    s.get("message.personal_welcome")).queue();
            tc.sendMessage(String.format(s.get("message.request_desc"), desc)).queue();

        } else if (event.getModalId().startsWith("request_form_")) {
            event.deferReply().queue();
            int agreement = Integer.parseInt(event.getModalId().split("_")[2]);

            String rNumS = extractModalValue(event, "request_num");
            String body = extractModalValue(event, "request_body");
            String target = extractModalValue(event, "request_target");
            String deadline = extractModalValue(event, "request_deadline");

            Advocate advocate = advocateSearch(event);
            if (advocate == null) return;

            int rNum;
            try {
                if (rNumS == null) throw new RuntimeException();
                rNum = Integer.parseInt(rNumS);
            } catch (Exception e) {
                event.getHook().sendMessage(s.get("message.request_wrong_num")).queue(MSG_DELETE_10);
                return;
            }

            //noinspection DataFlowIssue
            byte[] request = DocUtils.generateRequest(advocate.getName(), event.getUser().getName(), advocate.getPhone(),
                    advocate.getSignature(), deadline, target, body, agreement, rNum);
            Set<FileUpload> files = Collections.singleton(
                    FileUpload.fromData(new ByteArrayInputStream(request), "mba_lg_request_%d.jpg".formatted(rNum)));
            event.getHook().sendFiles(files).queue();
        } else if (event.getModalId().startsWith("agreement_link_")) {
            event.deferReply(true).queue();
            if (event.getMessage() != null)
                event.getMessage().editMessageComponents(Collections.emptyList()).queue();
            int agreement = Integer.parseInt(event.getModalId().split("_")[2]);

            String link = extractModalValue(event, "agreement_link");

            Agreement a = db.getAgreementById(agreement);
            if (a == null) {
                event.getHook().sendMessage(s.get("cmd.err.agreement_nf")).queue(MSG_DELETE_10);
                return;
            }

            Client c = db.getClientByPass(a.getClient());
            if (c != null) {
                TextChannel tc = DsUtils.getChannelById(c.getDsUserChannel());
                if (tc != null) {
                    tc.getManager().setTopic("%s, ссылка: %s".formatted(
                            Strings.getInstance().get("str.personal_channel_topic_pass_ag")
                                    .formatted(c.getPassport(), agreement),
                            link)).queue();
                }
            }

            event.getHook().sendMessage(s.get("str.agreement_published")).queue(MSG_DELETE_10);
        }
    }

    private @Nullable String extractModalValue(@NotNull ModalInteractionEvent event, String id) {
        ModalMapping m = event.getValue(id);
        if (m == null) return null;
        return m.getAsString();
    }

    private Advocate advocateSearch(IReplyCallback event) {
        if (DsUtils.hasNotAdvocatePerms(event.getMember())) {
            event.getHook().sendMessage(s.get("cmd.err.no_perm")).queue(MSG_DELETE_10);
            return null;
        }
        Advocate advocate = db.getAdvocateByDiscord(event.getUser().getIdLong());
        if (advocate == null || advocate.getSignature() == null || advocate.isNotActive()) {
            logAdvocateNf(event.getUser().getName());
            event.getHook().sendMessage(s.get("cmd.err.no_perm")).queue(MSG_DELETE_10);
        }
        return advocate;
    }

    private void logAdvocateNf(String name) {
        log.warn("Could not find advocate for user {}", name);
    }

    private void checkNotNull(Object... oa) {
        for (Object o : oa)
            if (o == null)
                throw new NullPointerException();
    }

}
