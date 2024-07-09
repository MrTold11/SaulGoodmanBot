package com.mrtold.saulgoodman.logic.lawrequest;

import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.discord.DsUtils;
import com.mrtold.saulgoodman.logic.model.Advocate;
import com.mrtold.saulgoodman.logic.model.Agreement;
import com.mrtold.saulgoodman.logic.model.Client;
import com.mrtold.saulgoodman.utils.Strings;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.mrtold.saulgoodman.discord.DsUtils.MSG_DELETE_10;

/**
 * @author Mr_Told
 */
public class LawRequestManager {

    static LawRequestManager instance;

    public static LawRequestManager getInstance() {
        if (instance == null)
            throw new NullPointerException("LawRequestManager has not been initialized yet.");
        return instance;
    }

    public static void init() {
        instance = new LawRequestManager();
    }

    //user id -> [channel id -> draft]
    final Map<Long, Map<Long, LawRequestDraft>> drafts = new ConcurrentHashMap<>();

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
            } catch (Exception ignored) {}
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
