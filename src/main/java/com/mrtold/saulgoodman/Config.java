package com.mrtold.saulgoodman;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Mr_Told
 */
public class Config {

    static final Config instance = new Config();

    public static Config getInstance() {
        return instance;
    }

    private long clientRoleId, advocateRoleId, headsRoleId;
    private long registryChannelId, auditChannelId, requestChannelId, requestsChannelId,
        firstAidChannelId, shiftChannelId;
    private long guildId;
    private int defaultBillAmount;
    private String clientsCategory, archiveCategory;

    private String discordToken, discordClientId, discordClientSecret, oAuth2Redirect,
            imgurClientId, dbHost, dbName, dbUser, dbPass;
    private int dbPort, apiPort;

    public Config load(File configFile) throws IOException {
        JsonObject json = JsonParser.parseReader(new FileReader(configFile, StandardCharsets.UTF_8)).getAsJsonObject();

        JsonObject discord = json.get("discord").getAsJsonObject();
        discordClientId = discord.get("client_id").getAsString();
        discordClientSecret = discord.get("client_secret").getAsString();
        oAuth2Redirect = discord.get("oauth2_redirect").getAsString();
        discordToken = discord.get("bot_token").getAsString();
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
        firstAidChannelId = discordChannels.get("first_aid").getAsLong();
        shiftChannelId = discordChannels.get("shift").getAsLong();

        JsonObject discordCategories = discord.get("categories").getAsJsonObject();
        clientsCategory = discordCategories.get("clients").getAsString();
        archiveCategory = discordCategories.get("archive").getAsString();

        JsonObject imgur = json.get("imgur").getAsJsonObject();
        imgurClientId = imgur.get("client_id").getAsString();

        JsonObject api = json.get("api").getAsJsonObject();
        apiPort = api.get("port").getAsInt();

        JsonObject database = json.get("database").getAsJsonObject();
        dbHost = database.get("host").getAsString();
        dbName = database.get("name").getAsString();
        dbUser = database.get("user").getAsString();
        dbPass = database.get("password").getAsString();
        dbPort = database.get("port").getAsInt();
        return this;
    }

    String getDiscordClientId() {
        return discordClientId;
    }

    String getDiscordClientSecret() {
        return discordClientSecret;
    }

    String getOAuth2Redirect() {
        return oAuth2Redirect;
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

    int getApiPort() {
        return apiPort;
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

    public long getFirstAidChannelId() {
        return firstAidChannelId;
    }

    public long getShiftChannelId() {
        return shiftChannelId;
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
