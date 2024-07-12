package com.mrtold.saulgoodman.services;

import java.util.HashMap;
import java.util.Map;

import org.apache.hc.client5.http.HttpResponseException;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class Authentication {
    static final String DISCORD_API_VERSION = "v10";
    static final String DISCORD_BASE_URL = String.format("https://discord.com/api/%s/", DISCORD_API_VERSION);
    static final String ACCESS_TOKEN_URL = DISCORD_BASE_URL + "oauth2/token";
    static final String GET_USER_URL = DISCORD_BASE_URL + "users/@me";

    final Logger log = LoggerFactory.getLogger(Authentication.class);

    final Map<String, Long> authenticatedUsers = new HashMap<>();

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public Authentication(String dsClientId, String dsClientSecret, String oAuth2Redirect) {
        clientId = dsClientId;
        clientSecret = dsClientSecret;
        redirectUri = oAuth2Redirect;
    }

    public Long authenticate(String code) {
        log.info("Authenticating by code : {}", code);
        log.info("Authenticated user by now : {}", authenticatedUsers);

        Long discordId = authenticatedUsers.get(code);
        if (discordId == null)
            discordId = this.getDiscordId(this.getToken(code));
        
        return discordId;
    }

    private String getToken(String code) {
        HttpPost post = new HttpPost(ACCESS_TOKEN_URL);
        String tokenBody = String.format(
                "client_id=%s&client_secret=%s&grant_type=authorization_code&code=%s&redirect_uri=%s",
                clientId, clientSecret, code, redirectUri
        );

        post.setEntity(new StringEntity(tokenBody));
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        
        try {
            JsonObject json = executeRequest(post);
            log.info("AUTHENTICATION -> GET TOKEN -> JSON : {}", json.toString());

            return json.get("access_token").getAsString();
        } catch (Exception e) {
            log.error("ERROR DURING GETTING AN ACCESS TOKEN TOKEN : ", e);
            return null;
        }

    }

    private Long getDiscordId(String token) {
        HttpGet get = new HttpGet(GET_USER_URL);
        get.setHeader("Authorization", "Bearer " + token);

        try {
            JsonObject json = executeRequest(get);

            log.info("AUTHENTICATION -> GET DISCORD ID -> JSON : {}", json.toString());

            return Long.parseLong(json.get("id").getAsString());
        } catch (Exception e) {
            log.error("ERROR: ", e);
            return null;
        }

    }

    private JsonObject executeRequest(ClassicHttpRequest request) throws HttpException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            Object response = httpclient.execute(request, r ->
                r.getCode() == HttpStatus.SC_OK ? EntityUtils.toString(r.getEntity()) : r.getCode());

            log.info("RESPONSE OF EXECUTING REQUEST ON AUTHENTICATION : {}", response.toString());

            if (response instanceof Integer rCode)
                throw new HttpResponseException(rCode, "Invalid response.");

            if (response instanceof String rString)
                return JsonParser.parseString(rString).getAsJsonObject();

            throw new JsonParseException("Response is neither Integer or String. Impossible to parse.");
        } catch (Exception e) {
            throw new HttpException("Exception during HTTP Request:", e);
        }
    }
}