package com.mrtold.saulgoodman.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Mr_Told
 */
public class Strings {

    static final Strings instance = new Strings();

    public static Strings getInstance() {
        return instance;
    }

    Map<String, String> dict = new HashMap<>();

    public Strings load(@NotNull File file, @Nullable Consumer<Strings> postOverride) throws FileNotFoundException {
        if (!dict.isEmpty())
            dict = new HashMap<>();

        JsonObject json = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : json.entrySet())
            addJsonEntry(entry.getKey(), entry.getValue());

        if (postOverride != null)
            postOverride.accept(this);

        return this;
    }

    private void addJsonEntry(String key, JsonElement value) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            if (dict.containsKey(key))
                throw new IllegalArgumentException(
                        String.format(Locale.getDefault(), "String key is already assigned: %s", key));
            dict.put(key, value.getAsString());
        } else if (value.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : value.getAsJsonObject().entrySet())
                addJsonEntry(key + "." + e.getKey(), e.getValue());
        }
    }

    public String get(String key) {
        return dict.get(key);
    }

    public Strings override(String key, String value) {
        dict.put(key, value);
        return this;
    }

}
