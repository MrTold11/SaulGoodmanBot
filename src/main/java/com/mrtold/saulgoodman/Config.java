package com.mrtold.saulgoodman;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * @author Mr_Told
 */
public class Config {

    static final Config instance = new Config();

    public static Config getInstance() {
        return instance;
    }

    private long clientRoleId, advocateRoleId, headsRoleId;
    private long registryChannelId, auditChannelId, requestChannelId, requestsChannelId;
    private long guildId;
    private int defaultBillAmount;
    private String clientsCategory, archiveCategory;

    private String discordToken, imgurClientId, dbHost, dbName, dbUser, dbPass;
    private int dbPort;

    public Config load(File configFile) throws FileNotFoundException {
        JsonObject json = JsonParser.parseReader(new FileReader(configFile)).getAsJsonObject();

        JsonObject discord = json.get("discord").getAsJsonObject();
        discordToken = discord.get("token").getAsString();
        guildId = discord.get("guild").getAsLong();
        defaultBillAmount = discord.get("default_bill_amount").getAsInt();

        JsonObject discordRoles = discord.get("roles").getAsJsonObject();
        clientRoleId = discordRoles.get("client").getAsLong();
        advocateRoleId = discordRoles.get("advocate").getAsLong();
        headsRoleId = discordRoles.get("head").getAsLong();

        JsonObject discordChannels = discord.get("channels").getAsJsonObject();
        registryChannelId = discordChannels.get("registry").getAsLong();
        auditChannelId = discordChannels.get("audit").getAsLong();
        requestChannelId = discordChannels.get("request").getAsLong();
        requestsChannelId = discordChannels.get("requests").getAsLong();

        JsonObject discordCategories = discord.get("categories").getAsJsonObject();
        clientsCategory = discordCategories.get("clients").getAsString();
        archiveCategory = discordCategories.get("archive").getAsString();

        JsonObject imgur = json.get("imgur").getAsJsonObject();
        imgurClientId = imgur.get("client_id").getAsString();

        JsonObject database = json.get("database").getAsJsonObject();
        dbHost = database.get("host").getAsString();
        dbName = database.get("name").getAsString();
        dbUser = database.get("user").getAsString();
        dbPass = database.get("password").getAsString();
        dbPort = database.get("port").getAsInt();
        return this;
    }

    String getDiscordToken() {
        return discordToken;
    }

    String getImgurClientId() {
        return imgurClientId;
    }

    String getDbHost() {
        return dbHost;
    }

    String getDbName() {
        return dbName;
    }

    String getDbUser() {
        return dbUser;
    }

    String getDbPass() {
        return dbPass;
    }

    int getDbPort() {
        return dbPort;
    }

    public long getGuildId() {
        return guildId;
    }

    public int getDefaultBillAmount() {
        return defaultBillAmount;
    }

    public long getRequestsChannelId() {
        return requestsChannelId;
    }

    public long getRequestChannelId() {
        return requestChannelId;
    }

    public long getClientRoleId() {
        return clientRoleId;
    }

    public long getAdvocateRoleId() {
        return advocateRoleId;
    }

    public long getRegistryChannelId() {
        return registryChannelId;
    }

    public long getAuditChannelId() {
        return auditChannelId;
    }

    public long getHeadsRoleId() {
        return headsRoleId;
    }

    public String getClientsCategory() {
        return clientsCategory;
    }

    public String getArchiveCategory() {
        return archiveCategory;
    }
}
