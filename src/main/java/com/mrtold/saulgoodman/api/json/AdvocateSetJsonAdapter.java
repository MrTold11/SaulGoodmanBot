package com.mrtold.saulgoodman.api.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mrtold.saulgoodman.logic.model.Advocate;

import java.io.IOException;
import java.util.Set;

public class AdvocateSetJsonAdapter extends TypeAdapter<Set<Advocate>> {

    @Override
    public void write(JsonWriter out, Set<Advocate> value) throws IOException {
        out.beginArray();
        for (Advocate a : value)
            out.value(a.getPassport());
        out.endArray();
    }

    @Override
    public Set<Advocate> read(JsonReader in) throws IOException {
        return null;
    }
}