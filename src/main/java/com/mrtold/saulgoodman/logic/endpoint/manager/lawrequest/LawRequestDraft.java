package com.mrtold.saulgoodman.logic.endpoint.manager.lawrequest;

/**
 * @author Mr_Told
 */
public class LawRequestDraft {

    final long advocateId, channelId;
    Long buttonMessage;

    int agreement;

    public LawRequestDraft(long advocateId, long channelId, int agreement) {
        this.advocateId = advocateId;
        this.channelId = channelId;
        this.agreement = agreement;
    }

    public long getAdvocateId() {
        return advocateId;
    }

    public long getChannelId() {
        return channelId;
    }

    public Long getButtonMessage() {
        return buttonMessage;
    }

    public void setButtonMessage(Long buttonMessage) {
        this.buttonMessage = buttonMessage;
    }

    public int getAgreement() {
        return agreement;
    }

    public void setAgreement(int agreement) {
        this.agreement = agreement;
    }
}
