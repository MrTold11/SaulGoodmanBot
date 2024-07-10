package com.mrtold.saulgoodman.logic.endpoint.manager.firstaid;

import com.mrtold.saulgoodman.logic.model.Client;

/**
 * @author Mr_Told
 */
public class FirstAidRequest {

    final long start;
    final Client client;

    public FirstAidRequest(long start, Client client) {
        this.start = start;
        this.client = client;
    }

    public FirstAidRequest(Client client) {
        this.start = System.currentTimeMillis();
        this.client = client;
    }

    public long getStart() {
        return start;
    }

    public Client getClient() {
        return client;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - start > 1000 * 60 * 60;
    }

}
