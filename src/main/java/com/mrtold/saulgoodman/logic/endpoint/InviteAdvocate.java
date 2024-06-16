package com.mrtold.saulgoodman.logic.endpoint;

import com.mrtold.saulgoodman.discord.DsUtils;
import com.mrtold.saulgoodman.logic.model.Advocate;
import com.mrtold.saulgoodman.logic.model.Agreement;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.Date;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * @author Mr_Told
 */
public class InviteAdvocate extends Endpoint {

    final long authorDsId;
    final long advocateDsId;
    final Integer advocatePass;
    final String advocateName;
    final Supplier<byte[]> advocateSignatureSupplier;

    public InviteAdvocate(long authorDsId, long advocateDsId, Integer advocatePass, String advocateName,
                          Supplier<byte[]> advocateSignatureSupplier) {
        this.authorDsId = authorDsId;
        this.advocateDsId = advocateDsId;
        this.advocatePass = advocatePass;
        this.advocateName = advocateName;
        this.advocateSignatureSupplier = advocateSignatureSupplier;
    }

    @Override
    public void execute() {
        if (DsUtils.hasNotHighPermission(authorDsId)) {
            onFailureEP(s.get("cmd.err.no_perm"));
            return;
        }

        byte[] signature = advocateSignatureSupplier.get();

        Advocate advocate = db.getAdvocateByPass(advocatePass);
        if (advocate == null) {
            advocate = new Advocate(advocatePass, advocateDsId, advocateName, signature);
        } else {
            advocate.setName(advocateName);
            advocate.setSignature(signature);
            advocate.setDsUserId(advocateDsId);
            advocate.setActive(1);
        }

        db.saveAdvocate(advocate);

        //generate system agreement with system user
        Agreement a1 = db.getAgreementById(-advocatePass);
        if (a1 == null)
            a1 = new Agreement(-advocatePass, new Date(), 1, advocatePass, 0);
        else
            a1.setStatus(1);
        db.saveAgreement(a1);

        DsUtils.getAuditChannel().sendMessage(MessageCreateData.fromEmbeds(
                DsUtils.prepareEmbedBuilder(15132410, s.get("embed.title.invite"))
                        .setDescription(String.format(Locale.getDefault(),
                                s.get("embed.body.invite"),
                                DsUtils.getMemberAsMention(advocateDsId),
                                advocateName,
                                advocatePass,
                                DsUtils.getMemberAsMention(authorDsId)))
                        .build())).queue();

        DsUtils.addRoleToMember(advocateDsId, config.getAdvocateRoleId());
        onSuccessEP();
    }

}
