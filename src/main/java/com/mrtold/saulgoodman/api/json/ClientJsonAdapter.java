package com.mrtold.saulgoodman.api.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mrtold.saulgoodman.logic.model.Client;

import java.io.IOException;

/**
 * @author Mr_Told
 */
public class ClientJsonAdapter extends TypeAdapter<Client> {

    @Override
    public void write(JsonWriter out, Client value) throws IOException {
        out.beginObject().name("passport").value(value.getPassport()).endObject();
    }

    @Override
    public Client read(JsonReader in) throws IOException {
        return null;
    }
}
