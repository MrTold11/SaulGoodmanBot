package com.mrtold.saulgoodman.logic.endpoint;

import com.mrtold.saulgoodman.discord.DsUtils;
import com.mrtold.saulgoodman.logic.model.Advocate;
import com.mrtold.saulgoodman.logic.model.Client;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * @author Mr_Told
 */
public class UninviteAdvocate extends Endpoint {

    final long advocateDsId;
    Long targetDsId;
    final Integer targetPass;
    final String reason;

    public UninviteAdvocate(long advocateDsId, Long targetDsId, Integer targetPass, String reason) {
        this.advocateDsId = advocateDsId;
        this.targetDsId = targetDsId;
        this.targetPass = targetPass;
        this.reason = reason;
    }

    @Override
    public void execute() {
        if (DsUtils.hasNotHighPermission(advocateDsId)) {
            onFailureEP(s.get("cmd.err.no_perm"));
            return;
        }

        Advocate advocate = db.getAdvocateByPass(targetPass);
        if (advocate == null)
            advocate = db.getAdvocateByDiscord(targetDsId);


        if (advocate == null || advocate.isNotActive()) {
            onFailureEP(s.get("cmd.err.advocate_nf"));
            return;
        }

        targetDsId = advocate.getDsUserId();
        advocate.setResigned(new Date());
        db.saveAdvocate(advocate);

        MessageCreateData mcd = MessageCreateData.fromEmbeds(
                DsUtils.prepareEmbedBuilder(14357564, s.get("embed.title.uninvite"))
                        .setDescription(String.format(Locale.getDefault(),
                                s.get("embed.body.uninvite"),
                                DsUtils.getMemberAsMention(targetDsId),
                                advocate.getName(),
                                advocate.getPassport(),
                                reason,
                                DsUtils.getMemberAsMention(advocateDsId))).build());

        DsUtils.getAuditChannel().sendMessage(mcd).queue();

        DsUtils.removeRoleFromMember(targetDsId, config.getAdvocateRoleId());

        Member member = DsUtils.getGuildMember(targetDsId);
        if (member != null) {
            List<Client> clients = db.getAllClients();
            for (Client client : clients) {
                if (Objects.equals(client.getDsUserId(), targetDsId)) continue;
                TextChannel channel = DsUtils.getChannelById(client.getDsUserChannel());
                if (channel != null)
                    channel.upsertPermissionOverride(member).clear(Permission.VIEW_CHANNEL).queue();
            }
        }
        onSuccessEP();
    }

}
