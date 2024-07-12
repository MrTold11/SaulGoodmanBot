package com.mrtold.saulgoodman.logic.manager.lawrequest;

import com.mrtold.saulgoodman.Main;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.discord.DsUtils;
import com.mrtold.saulgoodman.discord.event.DiscordEventManager;
import com.mrtold.saulgoodman.logic.manager.AbstractLogicManager;
import com.mrtold.saulgoodman.logic.model.Advocate;
import com.mrtold.saulgoodman.logic.model.Agreement;
import com.mrtold.saulgoodman.logic.model.Client;
import com.mrtold.saulgoodman.utils.DocUtils;
import com.mrtold.saulgoodman.utils.Strings;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.mrtold.saulgoodman.discord.DsUtils.MSG_DELETE_10;
import static com.mrtold.saulgoodman.discord.DsUtils.extractModalValue;

/**
 * @author Mr_Told
 */
public class LawRequestManager extends AbstractLogicManager {

    public static void init() {
        new LawRequestManager();
    }

    //user id -> [channel id -> draft]
    final Map<Long, Map<Long, LawRequestDraft>> drafts = new ConcurrentHashMap<>();

    @Override
    protected void initEventListeners() {
        DiscordEventManager.addButtonListenerStartsWith("lrd_", (e, args) -> {
            long userId = e.getUser().getIdLong();
            long channelId = e.getChannelIdLong();
            if (args[1].equals("cancel")) {
                purgeDraft(userId, channelId);
            } else if (args[1].equals("open")) {
                LawRequestDraft draft = getDraft(userId, channelId);
                if (draft == null) {
                    e.reply(Strings.getS("str.request_nf")).setEphemeral(true).queue();
                    e.getMessage().delete().queue();
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

                Modal modal = Modal.create("request_form_%d".formatted(draft.getAgreement()),
                                Strings.getS("embed.modal.request"))
                        .addComponents(
                                ActionRow.of(reqFormNum), ActionRow.of(reqFormBody),
                                ActionRow.of(reqFormTarget), ActionRow.of(reqFormDeadline))
                        .build();
                e.replyModal(modal).queue();
            }
        });

        DiscordEventManager.addModalListenerStartsWith("request_form_", e -> {
            e.deferReply().queue();
            int agreement = Integer.parseInt(e.getModalId().split("_")[2]);

            String rNumS = extractModalValue(e, "request_num");
            String body = extractModalValue(e, "request_body");
            String target = extractModalValue(e, "request_target");
            String deadline = extractModalValue(e, "request_deadline");

            Advocate advocate = advocateSearch(e);
            if (advocate == null) return;

            int rNum;
            try {
                if (rNumS == null) throw new RuntimeException();
                rNum = Integer.parseInt(rNumS);
            } catch (Exception ex) {
                e.getHook().sendMessage(Strings.getS("message.request_wrong_num")).queue(MSG_DELETE_10);
                return;
            }

            //noinspection DataFlowIssue
            byte[] request = DocUtils.generateRequest(advocate.getName(), e.getUser().getName(), advocate.getPhone(),
                    advocate.getSignature(), deadline, target, body, agreement, rNum);
            Set<FileUpload> files = Collections.singleton(
                    FileUpload.fromData(new ByteArrayInputStream(request), "mba_lg_request_%d.jpg".formatted(rNum)));
            e.getHook().sendMessage(body).addFiles(files).queue();
        });

        DiscordEventManager.addSlashCommandListener(Main.CMD_REQUEST, this::onRequestCommand);
    }

    @NotNull
    private Map<Long, LawRequestDraft> getUserDrafts(long userId) {
        return drafts.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
    }

    public LawRequestDraft createDraft(long userId, long channelId, int agreement) {
        LawRequestDraft draft = new LawRequestDraft(userId, channelId, agreement);
        checkDraftOverride(getUserDrafts(userId).put(channelId, draft));
        return draft;
    }

    @Nullable
    public LawRequestDraft getDraft(long userId, long channelId) {
        return getUserDrafts(userId).get(channelId);
    }

    public void purgeDraft(long userId, long channelId) {
        checkDraftOverride(getUserDrafts(userId).remove(channelId));
    }

    private void checkDraftOverride(@Nullable LawRequestDraft draft) {
        if (draft == null) return;

        if (draft.getButtonMessage() != null) {
            try {
                Message m = DsUtils.getMessageById(draft.getChannelId(), draft.getButtonMessage());
                if (m != null) m.delete().queue();
            } catch (Exception i) {
                log.warn("draft message remove exception", i);
            }
        }
    }

    public void onRequestCommand(GenericCommandInteractionEvent event) {
        event.deferReply(true).queue();
        DatabaseConnector db = DatabaseConnector.getInstance();
        Strings s = Strings.getInstance();
        long userId = event.getUser().getIdLong();
        Advocate advocate = db.getAdvocateByDiscord(userId);
        if (advocate == null || advocate.isNotActive()) {
            event.getHook().sendMessage(s.get("cmd.err.no_perm")).queue(MSG_DELETE_10);
            return;
        }

        Client client = db.getClientByChannel(event.getChannelIdLong());
        if (client == null) {
            event.getHook().sendMessage(s.get("cmd.err.client_nf")).queue(MSG_DELETE_10);
            return;
        }

        Agreement agreement = db.getActiveAgreement(client.getPassport());
        if (agreement == null || agreement.getStatus() != 1) {
            event.getHook().sendMessage(s.get("cmd.err.agreement_nf")).queue(MSG_DELETE_10);
            return;
        }

        LawRequestDraft draft = createDraft(userId, event.getChannelIdLong(), agreement.getNumber());

        event.getHook().sendMessage(s.get("embed.title.law_request")).setActionRow(
                Button.success("lrd_open", s.get("embed.button.edit")),
                Button.danger("lrd_cancel", s.get("embed.button.cancel"))
        ).queue(m -> draft.setButtonMessage(m.getIdLong()));
    }

}
