package com.mrtold.saulgoodman.logic.endpoint.attach;

import com.mrtold.saulgoodman.logic.model.Advocate;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mr_Told
 */
public class AttachName extends AttachEP {

    final String name;

    public AttachName(long advocateDsId, String name) {
        super(advocateDsId);
        this.name = name;
    }

    @Override
    protected void executeAdvocate(@NotNull Advocate advocate) {
        advocate.setName(name);
        onSuccessEP(advocate);
    }

}
