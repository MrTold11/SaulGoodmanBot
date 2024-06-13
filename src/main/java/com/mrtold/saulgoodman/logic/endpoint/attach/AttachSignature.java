package com.mrtold.saulgoodman.logic.endpoint.attach;

import com.mrtold.saulgoodman.logic.model.Advocate;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * @author Mr_Told
 */
public class AttachSignature extends AttachEP {

    final Supplier<byte[]> signatureSupplier;

    public AttachSignature(long advocateDsId, Supplier<byte[]> signatureSupplier) {
        super(advocateDsId);
        this.signatureSupplier = signatureSupplier;
    }

    @Override
    protected void executeAdvocate(@NotNull Advocate advocate) {
        advocate.setSignature(signatureSupplier.get());
        onSuccessEP(advocate);
    }

}
