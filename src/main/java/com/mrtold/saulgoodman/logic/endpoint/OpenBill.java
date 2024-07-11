package com.mrtold.saulgoodman.logic.endpoint;

import com.mrtold.saulgoodman.discord.DsUtils;
import com.mrtold.saulgoodman.logic.dsregistry.RegistryUpdateUtil;
import com.mrtold.saulgoodman.logic.model.Advocate;
import com.mrtold.saulgoodman.logic.model.Client;
import com.mrtold.saulgoodman.logic.model.Receipt;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.Locale;

/**
 * @author Mr_Told
 */
public class OpenBill extends Endpoint {

    Long clientDsId;
    long advocateDsId;
    final int amount;
    Integer clientPass;

    TextChannel personalChannel;
    Advocate advocate;
    Client client;

    public OpenBill(Long clientDsId, long advocateDsId, Integer clientPass, int amount) {
        this.clientDsId = clientDsId;
        this.advocateDsId = advocateDsId;
        this.clientPass = clientPass;
        this.amount = amount;
    }

    public OpenBill(Client client, Advocate advocate, int amount) {
        this.client = client;
        this.advocate = advocate;
        this.amount = amount;
    }

    @Override
    public void execute() {
        if (DsUtils.hasNotAdvocatePerms(advocateDsId)) {
            onFailureEP(s.get("cmd.err.no_perm"));
            return;
        }

        if (advocate == null) {
            advocate = db.getAdvocateByDiscord(advocateDsId);
            if (advocate == null || advocate.getSignature() == null || advocate.isNotActive()) {
                log.error("Could not find advocate for user id {}", advocateDsId);
                onFailureEP(s.get("cmd.err.no_perm"));
                return;
            }
        }

        if (client == null) {
            client = db.getClient(clientDsId, clientPass);
            if (client == null) {
                onFailureEP(s.get("cmd.err.client_nf"));
                return;
            }
        }

        if (amount < 1) {
            onFailureEP(s.get("cmd.err.amount_low"));
            return;
        }

        if (personalChannel == null && client.getDsUserChannel() != null)
            personalChannel = DsUtils.getChannelById(client.getDsUserChannel());

        Receipt r = db.saveReceipt(
                new Receipt(0, advocate.getPassport(), client.getPassport(), amount, null));

        MessageCreateData mcd = MessageCreateData.fromEmbeds(
                DsUtils.prepareEmbedBuilder(15132410, s.get("embed.title.receipt"))
                        .setDescription(String.format(Locale.getDefault(),
                                s.get("embed.body.receipt"),
                                r.getId(),
                                DsUtils.getMemberAsMention(client.getDsUserId()),
                                client.getName(),
                                client.getPassport(),
                                DsUtils.getMemberAsMention(advocate.getDsUserId()),
                                amount,
                                s.get("setting.bureau_bank_account"))).build());

        r.setDs_id(DsUtils.getAuditChannel().sendMessage(mcd).setActionRow(
                Button.success("bill_payed_%d".formatted(r.getId()), s.get("embed.button.payed")))
                .complete().getIdLong()
        );
        db.saveReceipt(r);

        if (personalChannel != null)
            personalChannel.sendMessage(MessageCreateBuilder.from(mcd)
                    .setContent(DsUtils.getMemberAsMention(client.getDsUserId())).build()).queue();

        onSuccessEP();
        RegistryUpdateUtil.updateReceiptRegistry();
    }

}
