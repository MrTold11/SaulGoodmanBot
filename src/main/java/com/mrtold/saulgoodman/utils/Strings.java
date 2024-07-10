package com.mrtold.saulgoodman.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author Mr_Told
 */
public class Strings {

    static final Strings instance = new Strings();
    static final Logger logger = LoggerFactory.getLogger(Strings.class);

    public static Strings getInstance() {
        return instance;
    }

    Map<String, String> dict = new HashMap<>();
    Set<String> nullStr = new HashSet<>();

    public Strings load(@NotNull File file, @Nullable Consumer<Strings> override) throws IOException {
        if (!dict.isEmpty())
            dict = new HashMap<>();

        JsonObject json = JsonParser.parseReader(new FileReader(file, StandardCharsets.UTF_8)).getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : json.entrySet())
            addJsonEntry(entry.getKey(), entry.getValue());

        if (override != null)
            override.accept(this);

        return this;
    }

    private void addJsonEntry(String key, JsonElement value) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            if (dict.containsKey(key))
                throw new IllegalArgumentException(
                        String.format(Locale.getDefault(), "String key is already assigned: %s", key));
            dict.put(key, value.getAsString());
        } else if (value.isJsonNull()) {
            if (dict.containsKey(key))
                throw new IllegalArgumentException(
                        String.format(Locale.getDefault(), "String key is already assigned: %s", key));
            nullStr.add(key);
        } else if (value.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : value.getAsJsonObject().entrySet())
                addJsonEntry(key + "." + e.getKey(), e.getValue());
        }
    }

    public String get(String key) {
        String s = dict.get(key);
        if (s == null && !nullStr.contains(key))
            logger.warn("String {} not found!", key);
        return s;
    }

    public static String getS(String key) {
        return getInstance().get(key);
    }

    public Strings override(String key, String value) {
        dict.put(key, value);
        return this;
    }

}
