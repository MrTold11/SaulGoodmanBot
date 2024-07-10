package com.mrtold.saulgoodman.logic.manager;

import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.discord.DsUtils;
import com.mrtold.saulgoodman.logic.model.Advocate;
import com.mrtold.saulgoodman.utils.Strings;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mrtold.saulgoodman.discord.DsUtils.MSG_DELETE_10;

/**
 * @author Mr_Told
 */
public abstract class AbstractLogicManager {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    public AbstractLogicManager() {
        initEventListeners();
    }

    protected abstract void initEventListeners();

    protected Advocate advocateSearch(IReplyCallback event) {
        if (DsUtils.hasNotAdvocatePerms(event.getMember())) {
            event.getHook().sendMessage(Strings.getS("cmd.err.no_perm")).queue(MSG_DELETE_10);
            return null;
        }
        Advocate advocate = DatabaseConnector.getInstance()
                .getAdvocateByDiscord(event.getUser().getIdLong());
        if (advocate == null || advocate.getSignature() == null || advocate.isNotActive()) {
            log.warn("Could not find advocate for user {}", event.getUser().getName());
            event.getHook().sendMessage(Strings.getS("cmd.err.no_perm")).queue(MSG_DELETE_10);
        }
        return advocate;
    }

}
