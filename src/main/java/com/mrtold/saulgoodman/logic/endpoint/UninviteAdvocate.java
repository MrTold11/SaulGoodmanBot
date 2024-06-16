package com.mrtold.saulgoodman.logic.endpoint;

import com.mrtold.saulgoodman.discord.DsUtils;
import com.mrtold.saulgoodman.logic.model.Advocate;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.Locale;

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
        advocate.setActive(0);
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
        onSuccessEP();
    }

}
