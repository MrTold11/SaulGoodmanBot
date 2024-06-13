package com.mrtold.saulgoodman.logic.endpoint;

import com.mrtold.saulgoodman.discord.DiscordUtils;
import com.mrtold.saulgoodman.logic.dsregistry.RegistryUpdateUtil;
import com.mrtold.saulgoodman.logic.model.Client;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

/**
 * @author Mr_Told
 */
public class RenameClient extends Endpoint {

    final long advocateDsId;
    final int clientPass;
    final String name;

    public RenameClient(long advocateDsId, int clientPass, String name) {
        this.advocateDsId = advocateDsId;
        this.clientPass = clientPass;
        this.name = name;
    }

    @Override
    public void execute() {
        if (DiscordUtils.hasNotAdvocatePerms(advocateDsId)) {
            onFailureEP(s.get("cmd.err.no_perm"));
            return;
        }

        Client client = db.getClientByPass(clientPass);
        if (client == null) {
            onFailureEP(s.get("cmd.err.client_nf"));
            return;
        }

        if (client.getName().equals(name)) {
            onFailureEP(s.get("cmd.err.client_name_ok"));
            return;
        }

        client.setName(name);
        db.saveClient(client);

        TextChannel personalChannel = DiscordUtils.getChannelById(client.getDsUserChannel());
        if (personalChannel != null)
            personalChannel.getManager().setName(name).queue();

        onSuccessEP();
        RegistryUpdateUtil.updateClientsRegistry();
    }

}
