package com.mrtold.saulgoodman.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * @author Mr_Told
 */
public class ImgurUtils {

    final static String UPLOAD_URL = "https://api.imgur.com/3/image";

    final Logger log;
    final String clientId;
    final HttpClient httpClient = HttpClientBuilder.create().build();

    public ImgurUtils(String clientId) {
        this.clientId = clientId;
        this.log = LoggerFactory.getLogger(ImgurUtils.class);
    }

    public String uploadImage(String link) {
        String responseString = null;
        try {
            HttpPost post = new HttpPost(UPLOAD_URL);
            post.setHeader("Authorization", "Client-ID " + clientId);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("image", link, ContentType.TEXT_PLAIN);
            builder.addTextBody("type", "link", ContentType.TEXT_PLAIN);

            HttpEntity multipart = builder.build();
            post.setEntity(multipart);
            HttpResponse response = httpClient.execute(post);
            HttpEntity responseEntity = response.getEntity();
            responseString = new String(responseEntity.getContent().readAllBytes());
            String l = responseString.split("\"link\": \"")[1].split("\"", 2)[0];
            new URL(l);
            return l;
        } catch (Exception e) {
            log.error("Error while uploading image to imgur", e);
            if (responseString != null) {
                log.error("Got imgur response: {}", responseString);
            }
        }
        return null;
    }

}
