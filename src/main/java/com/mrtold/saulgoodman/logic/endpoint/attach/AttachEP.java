package com.mrtold.saulgoodman.logic.endpoint.attach;

import com.mrtold.saulgoodman.logic.endpoint.Endpoint;
import com.mrtold.saulgoodman.logic.model.Advocate;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * @author Mr_Told
 */
public abstract class AttachEP extends Endpoint {

    final long advocateDsId;
    final Supplier<Advocate> advocateSupplier;

    public AttachEP(long advocateDsId) {
        this.advocateDsId = advocateDsId;
        this.advocateSupplier = () -> db.getAdvocateByDiscord(advocateDsId);
    }

    @Override
    public final void execute() {
        Advocate advocate = advocateSupplier.get();
        if (advocate == null || advocate.getSignature() == null) {
            log.error("Could not find advocate for user id {}", advocateDsId);
            onFailureEP(s.get("cmd.err.no_perm"));
            return;
        }

        executeAdvocate(advocate);
    }

    protected void onSuccessEP(Advocate advocate) {
        db.saveAdvocate(advocate);
        super.onSuccessEP();
    }

    protected abstract void executeAdvocate(@NotNull Advocate advocate);

}
