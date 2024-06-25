package com.mrtold.saulgoodman.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
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
    
    final BasicCredentialsProvider accessTokenCredsProvider = new BasicCredentialsProvider();
    final List<NameValuePair> authBodyBase = new ArrayList<>(3);

    final Logger log = LoggerFactory.getLogger(Authentication.class);

    final Map<String, Long> authenticatedUsers = new HashMap<>();


    public Authentication(String dsClientId, String dsClientSecret, String oAuth2Redirect) {
        authBodyBase.add(new BasicNameValuePair("grant_type", "authorization_code"));
        authBodyBase.add(new BasicNameValuePair("redirect_uri", oAuth2Redirect));
        accessTokenCredsProvider.setCredentials(
                new AuthScope("discord.com", -1),
                new UsernamePasswordCredentials(dsClientId, dsClientSecret.toCharArray())
        );
    }

    public Long authenticate(String code) {
        
        Long discordId = authenticatedUsers.get(code);
        if (discordId == null) {
            discordId = this.getDiscordId(this.getToken(code));
        }
        
        return discordId;
    }

    private String getToken(String code) {
        HttpPost post = new HttpPost(ACCESS_TOKEN_URL);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        
        List<NameValuePair> bodyPairs = new ArrayList<>(authBodyBase);

        bodyPairs.add(new BasicNameValuePair("code", code));
        post.setEntity(new UrlEncodedFormEntity(bodyPairs));


        try {
            JsonObject json = executeRequest(post);
            return json.get("access_token").getAsString();
        } catch (Exception e) {
            log.error("ERORR: ", e);
            return null;
        }

    }

    private Long getDiscordId(String token) {
        HttpGet get = new HttpGet(GET_USER_URL);
        get.setHeader("Authorization", "Bearer " + token);

        try {
            JsonObject json = executeRequest(get);
            return Long.parseLong(json.get("id").getAsString());
        } catch (Exception e) {
            log.error("ERORR: ", e);
            return null;
        }

    }

    private JsonObject executeRequest(ClassicHttpRequest request) throws HttpException {

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            Object response = httpclient.execute(request, r ->
                r.getCode() == HttpStatus.SC_OK ? EntityUtils.toString(r.getEntity()) : r.getCode());

            if (response instanceof Integer) {
                throw new HttpResponseException(Integer.parseInt(response.toString()), "Invalid response.");
            }
            if (response instanceof String) {
                return JsonParser.parseString(response.toString()).getAsJsonObject();
            }
            else {
                throw new JsonParseException("Response is neither Integer or String. Impossible to parse.");
            }
            
        } catch (Exception e) {
            throw new HttpException("Exception during HTTP Request:", e);
        }
    }
}