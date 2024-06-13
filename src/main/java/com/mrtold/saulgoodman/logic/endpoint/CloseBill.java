package com.mrtold.saulgoodman.logic.endpoint;

import com.mrtold.saulgoodman.discord.DiscordUtils;
import com.mrtold.saulgoodman.logic.dsregistry.RegistryUpdateUtil;
import com.mrtold.saulgoodman.logic.model.Receipt;
import net.dv8tion.jda.api.entities.Message;

import java.util.Locale;
import java.util.Objects;

/**
 * @author Mr_Told
 */
public class CloseBill extends Endpoint {

    final long advocateDsId;
    Integer num;
    Message billMessage;

    public CloseBill(long advocateDsId, int num) {
        this.advocateDsId = advocateDsId;
        this.num = num;
    }

    public CloseBill(long advocateDsId, Message billMessage) {
        this.advocateDsId = advocateDsId;
        this.billMessage = billMessage;
    }

    @Override
    public void execute() {
        if (DiscordUtils.hasNotHighPermission(advocateDsId)) {
            onFailureEP(s.get("cmd.err.no_perm"));
            return;
        }

        Receipt receipt;
        try {
            if (num == null) num = Integer.parseInt(
                    Objects.requireNonNull(billMessage.getEmbeds().get(0).getDescription())
                            .split(s.get("embed.reflect.body.receipt.num_label"))[1]
                            .split(s.get("embed.reflect.body.receipt.separator"))[0]);
            receipt = db.getReceipt(num);
        } catch (Exception e) {
            receipt = null;
        }

        if (receipt == null) {
            onFailureEP(s.get("cmd.err.receipt_nf"));
            return;
        }

        receipt.setStatus(1);
        db.saveReceipt(receipt);

        if (billMessage == null && receipt.getDs_id() != null) {
            billMessage = DiscordUtils.getAuditChannel().getHistory()
                    .getMessageById(receipt.getDs_id());
        }

        if (billMessage != null)
            billMessage.editMessage(
                    String.format(Locale.getDefault(), s.get("message.payed_mark_by"),
                        DiscordUtils.getMemberAsMention(advocateDsId),
                        DiscordUtils.formatCurrentTime())
            ).setComponents().queue();

        onSuccessEP();
        RegistryUpdateUtil.updateReceiptRegistry();
    }

}
