package com.mrtold.saulgoodman.api.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mrtold.saulgoodman.logic.model.Claim;

import java.io.IOException;

/**
 * @author Mr_Told
 */
public class ClaimJsonAdapter extends TypeAdapter<Claim> {
    @Override
    public void write(JsonWriter out, Claim value) throws IOException {
        out.beginObject().name("claim").value(value.getId()).endObject();
    }

    @Override
    public Claim read(JsonReader in) throws IOException {
        return null;
    }
}
