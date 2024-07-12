package com.mrtold.saulgoodman.api;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mrtold.saulgoodman.logic.model.Evidence;

import java.io.IOException;

/**
 * @author Mr_Told
 */
public class EvidenceJsonAdapter extends TypeAdapter<Evidence> {
    @Override
    public void write(JsonWriter out, Evidence value) throws IOException {
        out.beginObject().name("id").value(value.getId()).endObject();
    }

    @Override
    public Evidence read(JsonReader in) throws IOException {
        return null;
    }
}
