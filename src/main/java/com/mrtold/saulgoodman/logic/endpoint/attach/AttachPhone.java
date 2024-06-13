package com.mrtold.saulgoodman.logic.endpoint.attach;

import com.mrtold.saulgoodman.logic.model.Advocate;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mr_Told
 */
public class AttachPhone extends AttachEP {

    final int phone;

    public AttachPhone(long advocateDsId, int phone) {
        super(advocateDsId);
        this.phone = phone;
    }

    @Override
    protected void executeAdvocate(@NotNull Advocate advocate) {
        advocate.setPhone(phone);
        onSuccessEP(advocate);
    }

}
