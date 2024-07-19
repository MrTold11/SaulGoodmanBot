package com.mrtold.saulgoodman.api;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mrtold.saulgoodman.logic.model.Evidence;

import java.io.IOException;
import java.util.Set;

public class EvidenceSetJsonAdapter extends TypeAdapter<Set<Evidence>> {

    @Override
    public void write(JsonWriter out, Set<Evidence> value) throws IOException {
        out.beginArray();
        for (Evidence e : value)
            out.value(e.getId());
        out.endArray();
    }

    @Override
    public Set<Evidence> read(JsonReader in) throws IOException {
        return null;
    }
}