package com.mrtold.saulgoodman.api.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mrtold.saulgoodman.logic.model.Advocate;

import java.io.IOException;

/**
 * @author Mr_Told
 */
public class AdvocateJsonAdapter extends TypeAdapter<Advocate> {
    @Override
    public void write(JsonWriter out, Advocate value) throws IOException {
        out.beginObject().name("passport").value(value.getPassport()).endObject();
    }

    @Override
    public Advocate read(JsonReader in) throws IOException {
        return null;
    }
}
