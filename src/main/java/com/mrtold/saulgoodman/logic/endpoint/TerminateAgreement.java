package com.mrtold.saulgoodman.logic.endpoint;

import com.mrtold.saulgoodman.discord.DiscordUtils;
import com.mrtold.saulgoodman.logic.dsregistry.RegistryUpdateUtil;
import com.mrtold.saulgoodman.logic.model.Agreement;
import com.mrtold.saulgoodman.logic.model.Client;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.Locale;

/**
 * @author Mr_Told
 */
public class TerminateAgreement extends Endpoint {

    final long advocateDsId;
    Long clientDsId;
    final Integer clientPass;
    final String reason;

    public TerminateAgreement(long advocateDsId, Long clientDsId, Integer clientPass, String reason) {
        this.advocateDsId = advocateDsId;
        this.clientDsId = clientDsId;
        this.clientPass = clientPass;
        this.reason = reason;
    }

    @Override
    public void execute() {
        if (DiscordUtils.hasNotHighPermission(advocateDsId)) {
            onFailureEP(s.get("cmd.err.no_perm"));
            return;
        }

        Client client = db.getClient(clientDsId, clientPass);
        if (client == null) {
            onFailureEP(s.get("cmd.err.client_nf"));
            return;
        }

        if (client.getDsUserId() != null)
            clientDsId = client.getDsUserId();

        Agreement a = db.getActiveAgreement(client.getPassport());
        if (a == null) {
            onFailureEP(s.get("cmd.err.client_nf"));
            return;
        }

        a.setStatus(0);
        db.saveAgreement(a);

        MessageCreateData mcd = MessageCreateData.fromEmbeds(
                DiscordUtils.prepareEmbedBuilder(14357564, s.get("embed.title.terminate"))
                        .setDescription(String.format(Locale.getDefault(),
                                s.get("embed.body.terminate"),
                                DiscordUtils.getMemberAsMention(clientDsId),
                                client.getName(),
                                client.getPassport(),
                                reason,
                                DiscordUtils.getMemberAsMention(advocateDsId))).build());

        DiscordUtils.getAuditChannel().sendMessage(mcd).queue();

        TextChannel personalChannel = DiscordUtils.getChannelById(client.getDsUserChannel());
        if (personalChannel != null) {
            personalChannel.sendMessage(mcd).queue();
            DiscordUtils.archivePersonalChannel(personalChannel);
        }

        DiscordUtils.removeRoleFromMember(clientDsId, config.getClientRoleId());
        onSuccessEP();
        RegistryUpdateUtil.updateClientsRegistry();
    }

}
