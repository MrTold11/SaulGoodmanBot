package com.mrtold.saulgoodman.api;

import com.google.gson.Gson;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.logic.model.Agreement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;
import spark.Spark;

import java.util.List;

import static spark.Spark.*;

/**
 * @author Mr_Told
 */
public class WebApi {

    static WebApi instance;

    public static WebApi getInstance() {
        if (instance == null)
            throw new IllegalStateException("WebApi not initialized");
        return instance;
    }

    public static WebApi init(int port) {
        instance = new WebApi(port);
        return instance;
    }

    final Logger log = LoggerFactory.getLogger(WebApi.class);
    DatabaseConnector db;

    WebApi(int port) {
        port(port);
        db = DatabaseConnector.getInstance();
        init();
    }

    private void init() {
        path("/demo", () -> {
            get("/agreements/:pass", agreementsByAdvocatePass);
        });
    }

    Route agreementsByAdvocatePass = (request, response) -> {
        try {
            int pass = Integer.parseInt(request.params("pass"));
            List<Agreement> agreements = db.getAgreementsByAdvocate(pass);
            Gson gson = new Gson();
            String json = gson.toJson(agreements);
            response.status(200);
            response.type("application/json");
            return json;
        } catch (Exception e) {
            log.warn("Exception on api process", e);
            halt(500);
            return null;
        }
    };

    public void close() {
        Spark.stop();
    }

}
