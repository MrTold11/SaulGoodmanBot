package com.mrtold.saulgoodman.logic.endpoint;

import com.mrtold.saulgoodman.discord.DiscordUtils;
import com.mrtold.saulgoodman.logic.model.Client;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

/**
 * @author Mr_Told
 */
public class RemoveClient extends Endpoint {

    final long advocateDsId;
    final int clientPass;

    TextChannel personalChannel;
    Client client;

    public RemoveClient(long advocateDsId, int clientPass) {
        this.advocateDsId = advocateDsId;
        this.clientPass = clientPass;
    }

    @Override
    public void execute() {
        if (DiscordUtils.hasNotHighPermission(advocateDsId)) {
            onFailureEP(s.get("cmd.err.no_perm"));
            return;
        }

        client = db.getClientByPass(clientPass);
        if (client == null) {
            onFailureEP(s.get("cmd.err.client_nf"));
            return;
        }
        if (db.clientHasActiveAgreement(clientPass)) {
            onFailureEP(s.get("cmd.err.already_has_agreement"));
            return;
        }

        log.info("Client with pass {} was removed by admin w/ discord id {}",
                clientPass, advocateDsId);

        if (client.getDsUserChannel() != null) {
            personalChannel = DiscordUtils.getChannelById(client.getDsUserChannel());
            if (personalChannel != null) personalChannel.delete().queue();
        }

        db.deleteClient(client);
        onSuccessEP();
    }

}
