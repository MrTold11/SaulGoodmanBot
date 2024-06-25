package com.mrtold.saulgoodman.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Mr_Told
 */
public class WebAuth {

    final static String DISCORD_API_VERSION = "v10";
    final static String DISCORD_BASE_URL = String.format("https://discord.com/api/%s/", DISCORD_API_VERSION);
    final static String ACCESS_TOKEN_URL = DISCORD_BASE_URL + "oauth2/token";
    final static String GET_USER_URL = DISCORD_BASE_URL + "users/@me";

    final Logger log = LoggerFactory.getLogger(WebAuth.class);

    final BasicCredentialsProvider accessTokenCredsProvider = new BasicCredentialsProvider();
    final List<NameValuePair> authBodyBase = new ArrayList<>(3);

    final Map<String, Long> authCache = new HashMap<>();

    public WebAuth(String dsClientId, String dsClientSecret, String oAuth2Redirect) {
        authBodyBase.add(new BasicNameValuePair("grant_type", "authorization_code"));
        authBodyBase.add(new BasicNameValuePair("redirect_uri", oAuth2Redirect));
        accessTokenCredsProvider.setCredentials(
                new AuthScope("discord.com", -1),
                new UsernamePasswordCredentials(dsClientId, dsClientSecret.toCharArray())
        );
    }

    public @Nullable Long auth(String accessCode) {
        if (accessCode == null || accessCode.isEmpty()) {
            log.warn("Auth Access code is null or empty");
            return null;
        }

        Long dsId = authCache.get(accessCode);
        if (dsId != null)
            return dsId;

        dsId = getDsIdByAccessToken(codeToAccessToken(accessCode));
        if (dsId != null)
            authCache.put(accessCode, dsId);

        return dsId;
    }

    private Long getDsIdByAccessToken(@Nullable String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("Auth Access token is null or empty");
            return null;
        }

        HttpGet get = new HttpGet(GET_USER_URL);
        get.setHeader("Authorization", "Bearer " + accessToken);
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String resp = httpclient.execute(get, r ->
                    r.getCode() == HttpStatus.SC_OK ? EntityUtils.toString(r.getEntity()) : null);
            if (resp != null) {
                JsonObject json = JsonParser.parseString(resp)
                        .getAsJsonObject();
                return Long.parseLong(json.get("id").getAsString());
            }
        } catch (Exception e) {
            log.error("Error while retrieving ds user id", e);
        }
        return null;
    }

    private String codeToAccessToken(@NotNull String code) {
        HttpPost post = new HttpPost(ACCESS_TOKEN_URL);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        List<NameValuePair> bodyPairs = new ArrayList<>(authBodyBase);
        bodyPairs.add(new BasicNameValuePair("code", code));
        post.setEntity(new UrlEncodedFormEntity(bodyPairs));
        try (CloseableHttpClient httpclient = HttpClients.custom().
                setDefaultCredentialsProvider(accessTokenCredsProvider).build()) {
            String resp = httpclient.execute(post, r ->
                    r.getCode() == HttpStatus.SC_OK ? EntityUtils.toString(r.getEntity()) : null);
            if (resp != null) {
                JsonObject json = JsonParser.parseString(resp)
                        .getAsJsonObject();
                return json.get("access_token").getAsString();
            }
        } catch (Exception e) {
            log.error("Error while retrieving access token", e);
        }
        return null;
    }

}
