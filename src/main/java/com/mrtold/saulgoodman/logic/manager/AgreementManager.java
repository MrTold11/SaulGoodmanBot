package com.mrtold.saulgoodman.logic.manager;

import com.mrtold.saulgoodman.Config;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.discord.DsUtils;
import com.mrtold.saulgoodman.discord.event.DiscordEventManager;
import com.mrtold.saulgoodman.logic.model.Advocate;
import com.mrtold.saulgoodman.logic.model.Agreement;
import com.mrtold.saulgoodman.logic.model.Client;
import com.mrtold.saulgoodman.utils.Strings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

import static com.mrtold.saulgoodman.discord.DsUtils.MSG_DELETE_10;
import static com.mrtold.saulgoodman.discord.DsUtils.extractModalValue;

/**
 * @author Mr_Told
 */
public class AgreementManager extends AbstractLogicManager {

    @Override
    protected void initEventListeners() {
        DatabaseConnector db = DatabaseConnector.getInstance();
        Strings s = Strings.getInstance();
        Config config = Config.getInstance();

        DiscordEventManager.addButtonListenerExact("agreement_request", (e, args) -> {
            Client client = db.getClientByDiscord(e.getUser().getIdLong());
            if (client != null) {
                e.deferReply(true).queue();
                e.getHook().sendMessage(s.get("message.already_client")).queue(MSG_DELETE_10);
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
            e.replyModal(modal).queue();
        });

        DiscordEventManager.addButtonListenerStartsWith("aReq_", (e, args) -> {
            e.deferReply(true).queue();
            Advocate advocate = advocateSearch(e);
            if (advocate == null) return;

            int pass = Integer.parseInt(args[2]);
            Client client = db.getClientByPass(pass);
            if (client == null || client.getDsUserChannel() == null || client.getDsUserId() == null) {
                e.getHook().sendMessage(s.get("cmd.err.client_nf")).queue(MSG_DELETE_10);
                return;
            }

            TextChannel tc = DsUtils.getChannelById(client.getDsUserChannel());
            if (tc == null) {
                db.deleteClient(client);
                e.getMessage().editMessage(s.get("str.request_cancelled"))
                        .setComponents().queue();
                e.getHook().sendMessage(s.get("str.request_cancelled")).queue(MSG_DELETE_10);
                return;
            }

            if (args[1].equals("acc")) {
                tc.upsertPermissionOverride(e.getMember()).grant(Permission.VIEW_CHANNEL).queue();
                e.getMessage().editMessage(s.get("str.request_accepted_by") +
                        Objects.requireNonNull(e.getMember()).getAsMention()).setComponents().queue();
                e.getHook().sendMessage(s.get("str.request_accepted")).queue(MSG_DELETE_10);
            } else {
                db.deleteClient(client);
                tc.delete().queue();
                e.getMessage().editMessage(s.get("str.request_declined_by") +
                        Objects.requireNonNull(e.getMember()).getAsMention()).setComponents().queue();
                e.getHook().sendMessage(s.get("str.request_declined")).queue(MSG_DELETE_10);
            }
        });

        DiscordEventManager.addButtonListenerStartsWith("agr_", (e, args) -> {
            boolean ok = args[1].equals("ok");
            long advocateId = Long.parseLong(args[2]);
            int num = Integer.parseInt(args[3]);

            Advocate advocate = db.getAdvocateByDiscord(advocateId);
            if (advocate == null) {
                log.warn("Could not find advocate for user {}", e.getUser().getName());
                e.reply(s.get("cmd.err.no_perm")).setEphemeral(true).queue();
                return;
            }

            if ((e.getUser().getIdLong() != advocateId && DsUtils.hasNotHighPermission(e.getMember()))
                    || DsUtils.hasNotAdvocatePerms(e.getMember())) {
                e.reply(s.get("cmd.err.no_perm")).setEphemeral(true).queue();
                return;
            }

            Agreement a = db.getAgreementById(num);

            if (a == null) {
                e.getMessage().editMessageComponents(Collections.emptyList()).queue();
                e.reply(s.get("cmd.err.agreement_nf")).setEphemeral(true).queue();
                return;
            }

            if (ok) {
                TextInput agreementLink = DsUtils.formTextInput("agreement_link", "link",
                        TextInputStyle.SHORT, 10, 150, true);

                Modal modal = Modal.create("agreement_link_%d".formatted(a.getNumber()), s.get("embed.modal.ag_link"))
                        .addComponents(ActionRow.of(agreementLink)).build();
                e.replyModal(modal).queue();
            } else {
                e.deferReply(true).queue();
                e.getMessage().editMessageComponents(Collections.emptyList()).queue();
                log.info("Agreement {} has been cancelled by advocate {}", num, e.getUser().getIdLong());
                db.deleteAgreement(a);
                e.getMessage().editMessage(s.get("str.rolled_back")).setFiles(Collections.emptyList()).queue();
                e.getHook().sendMessage(s.get("str.agreement_cancelled")).queue(MSG_DELETE_10);
            }
        });

        DiscordEventManager.addModalListenerExact("agreement_request_form", e -> {
            e.deferReply(true).queue();
            String name = extractModalValue(e, "agreement_request_name");
            String pass = extractModalValue(e, "agreement_request_pass");
            String desc = extractModalValue(e, "agreement_request_desc");
            int passport;
            long dsId = e.getUser().getIdLong();

            try {
                if (pass == null) throw new RuntimeException();
                passport = Integer.parseInt(pass);
                if (passport < 1) throw new RuntimeException();
            } catch (Exception ex) {
                e.getHook().sendMessage(s.get("message.request_wrong_pass"))
                        .queue(MSG_DELETE_10);
                return;
            }

            Client client = db.getClientByPass(passport);
            if (client != null || name == null || name.isBlank()) {
                e.getHook().sendMessage(s.get("message.request_failed"))
                        .queue(MSG_DELETE_10);
                return;
            }

            TextChannel tc = DsUtils.createPersonalChannel( null, "❕・" + name, dsId,
                    null, passport, null);
            client = new Client(passport, dsId, name, tc.getIdLong());

            DsUtils.getRequestsChannel()
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
            e.getHook().sendMessage(s.get("message.request_accepted")).queue(MSG_DELETE_10);
            tc.sendMessage(DsUtils.getMemberAsMention(dsId) +
                    s.get("message.personal_welcome")).queue();
            tc.sendMessage(String.format(s.get("message.request_desc"), desc)).queue();
        });

        DiscordEventManager.addModalListenerStartsWith("agreement_link_", e -> {
            e.deferReply(true).queue();
            if (e.getMessage() != null)
                e.getMessage().editMessageComponents(Collections.emptyList()).queue();
            int agreement = Integer.parseInt(e.getModalId().split("_")[2]);

            String link = extractModalValue(e, "agreement_link");

            Agreement a = db.getAgreementById(agreement);
            if (a == null) {
                e.getHook().sendMessage(s.get("cmd.err.agreement_nf")).queue(MSG_DELETE_10);
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

            e.getHook().sendMessage(s.get("str.agreement_published")).queue(MSG_DELETE_10);
        });
    }
}
