package com.mrtold.saulgoodman.api.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mrtold.saulgoodman.logic.model.Client;

import java.io.IOException;
import java.util.Set;

/**
 * @author Mr_Told
 */
public class ClientSetJsonAdapter extends TypeAdapter<Set<Client>> {

    @Override
    public void write(JsonWriter out, Set<Client> value) throws IOException {
        out.beginArray();
        for (Client c : value)
            out.value(c.getPassport());
        out.endArray();
    }

    @Override
    public Set<Client> read(JsonReader in) throws IOException {
        return null;
    }
}
