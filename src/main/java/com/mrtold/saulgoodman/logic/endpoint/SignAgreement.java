package com.mrtold.saulgoodman.logic.endpoint;

import com.mrtold.saulgoodman.discord.DsUtils;
import com.mrtold.saulgoodman.logic.dsregistry.RegistryUpdateUtil;
import com.mrtold.saulgoodman.logic.model.Advocate;
import com.mrtold.saulgoodman.logic.model.Agreement;
import com.mrtold.saulgoodman.logic.model.Client;
import com.mrtold.saulgoodman.utils.DocUtils;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * @author Mr_Told
 */
public class SignAgreement extends Endpoint {

    final long advocateDsId;
    final Long clientDsId;
    final String clientName;
    final int clientPass, agreementNum;
    final Supplier<byte[]> clientSignatureSupplier;

    public SignAgreement(long advocateDsId, Long clientDsId, String clientName, int clientPass, int agreementNum,
                         Supplier<byte[]> clientSignatureSupplier) {
        this.advocateDsId = advocateDsId;
        this.clientDsId = clientDsId;
        this.clientName = clientName;
        this.clientPass = clientPass;
        this.agreementNum = agreementNum;
        this.clientSignatureSupplier = clientSignatureSupplier;
    }

    @Override
    public void execute() {
        if (DsUtils.hasNotAdvocatePerms(advocateDsId)) {
            onFailureEP(s.get("cmd.err.no_perm"));
            return;
        }

        Advocate advocate = db.getAdvocateByDiscord(advocateDsId);
        if (advocate == null || advocate.getSignature() == null || advocate.isNotActive()) {
            log.error("Could not find advocate for user id {}", advocateDsId);
            onFailureEP(s.get("cmd.err.no_perm"));
            return;
        }

        if (db.clientHasActiveAgreement(clientPass)) {
            onFailureEP(s.get("cmd.err.already_has_agreement"));
            return;
        }

        if (db.getAgreementById(agreementNum) != null) {
            onFailureEP(s.get("cmd.err.agreement_exists"));
            return;
        }

        byte[] clientSignature = clientSignatureSupplier.get();
        byte[] agreement = DocUtils.generateAgreement(advocate.getName(), advocate.getPassport(),
                advocate.getSignature(), clientName, clientPass, clientSignature, agreementNum);
        List<FileUpload> files = Collections.singletonList(
                FileUpload.fromData(new ByteArrayInputStream(agreement), "mba_lg_%d.jpg".formatted(agreementNum)));

        Client client = db.getOrCreateClient(clientPass, clientDsId, clientName);
        TextChannel personalChannel = DsUtils.createPersonalChannel(client.getDsUserChannel(),
                clientName, clientDsId, advocate, clientPass);
        db.updateClient(client, clientDsId, clientName, personalChannel.getIdLong());

        db.saveAgreement(new Agreement(agreementNum, new Date(), 1, advocate.getPassport(), clientPass));

        MessageCreateData mcd = MessageCreateData.fromEmbeds(
                DsUtils.prepareEmbedBuilder(8453888, s.get("embed.title.sign"))
                        .setDescription(String.format(Locale.getDefault(),
                                s.get("embed.body.sign"),
                                DsUtils.getMemberAsMention(clientDsId),
                                clientName,
                                clientPass,
                                DsUtils.getMemberAsMention(advocateDsId))).build());

        DsUtils.getAuditChannel().sendMessage(mcd).addFiles(files)
                .setActionRow(
                        Button.success("agr_ok_%d_%d".formatted(advocateDsId, agreementNum), s.get("embed.button.published")),
                        Button.danger("agr_cancel_%d_%d".formatted(advocateDsId, agreementNum), s.get("embed.button.cancel")))
                .queue();
        personalChannel.sendMessage(mcd).addFiles(files).queue();
        DsUtils.addRoleToMember(clientDsId, config.getClientRoleId());

        onSuccessEP();

        new OpenBill(client, advocate, config.getDefaultBillAmount())
                .onFailure(s -> log.warn("Couldn't open bill on agreement sign: {}", s))
                .execute();
        RegistryUpdateUtil.updateClientsRegistry();
    }

}
