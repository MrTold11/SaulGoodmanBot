package com.mrtold.saulgoodman.logic.endpoint;

import com.mrtold.saulgoodman.discord.DsUtils;
import com.mrtold.saulgoodman.logic.dsregistry.RegistryUpdateUtil;
import com.mrtold.saulgoodman.logic.model.Receipt;
import net.dv8tion.jda.api.entities.Message;

import java.util.Locale;

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

    public CloseBill(long advocateDsId, Message billMessage, String idString) {
        this.advocateDsId = advocateDsId;
        this.billMessage = billMessage;
        try {
            num = Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            num = null;
        }
    }

    @Override
    public void execute() {
        if (DsUtils.hasNotHighPermission(advocateDsId)) {
            onFailureEP(s.get("cmd.err.no_perm"));
            return;
        }

        Receipt receipt = db.getReceipt(num);

        if (receipt == null) {
            onFailureEP(s.get("cmd.err.receipt_nf"));
            return;
        }

        receipt.setStatus(1);
        db.saveReceipt(receipt);

        if (billMessage == null && receipt.getDs_id() != null) {
            billMessage = DsUtils.getAuditChannel().getHistory()
                    .getMessageById(receipt.getDs_id());
        }

        if (billMessage != null)
            billMessage.editMessage(
                    String.format(Locale.getDefault(), s.get("message.payed_mark_by"),
                        DsUtils.getMemberAsMention(advocateDsId),
                        DsUtils.formatCurrentTime())
            ).setComponents().queue();

        onSuccessEP();
        RegistryUpdateUtil.updateReceiptRegistry();
    }

}
