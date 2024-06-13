package com.mrtold.saulgoodman.utils;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpEntityContainer;
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
            try (HttpEntity responseEntity = httpClient.execute(post, HttpEntityContainer::getEntity)) {
                responseString = new String(responseEntity.getContent().readAllBytes());
            }
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
