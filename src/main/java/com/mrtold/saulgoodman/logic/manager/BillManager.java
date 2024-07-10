package com.mrtold.saulgoodman.logic.manager;

import com.mrtold.saulgoodman.discord.event.DiscordEventManager;
import com.mrtold.saulgoodman.logic.endpoint.CloseBill;
import com.mrtold.saulgoodman.utils.Strings;

import static com.mrtold.saulgoodman.discord.DsUtils.MSG_DELETE_10;

/**
 * @author Mr_Told
 */
public class BillManager extends AbstractLogicManager {

    @Override
    protected void initEventListeners() {
        DiscordEventManager.addButtonListenerStartsWith("bill_payed_", (e, args) ->
                new CloseBill(e.getUser().getIdLong(), e.getMessage(), args[2])
                        .exec(() -> e.getHook()
                                        .sendMessage(Strings.getS("str.receipt_paid")).queue(MSG_DELETE_10),
                                m -> e.getHook()
                                        .sendMessage(m).queue(MSG_DELETE_10)));
    }

}
