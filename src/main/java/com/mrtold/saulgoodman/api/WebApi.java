package com.mrtold.saulgoodman.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.logic.model.*;
import org.jetbrains.annotations.NotNull;
import spark.Request;

import java.util.List;
import java.util.function.Function;

import static spark.Spark.*;

/**
 * @author Mr_Told
 */
public class WebApi {

    static WebApi instance;

    public static WebApi init(String dsClientId, String dsClientSecret, String oAuth2Redirect, int port) {
        instance = new WebApi(dsClientId, dsClientSecret, oAuth2Redirect, port);
        return instance;
    }

    final Gson gson = new Gson();
    final DatabaseConnector db;

    final WebAuth auth;

    WebApi(String dsClientId, String dsClientSecret, String oAuth2Redirect, int port) {
        auth = new WebAuth(dsClientId, dsClientSecret, oAuth2Redirect);
        db = DatabaseConnector.getInstance();
        port(port);
        init();
    }

    private void init() {
        initCommonGet("case", db::getCaseById);
        initCommonGet("receipt", db::getReceipt);
        initCommonGet("agreement", db::getAgreementById);
        initCommonGet("advocate", db::getAdvocateByPass);
        initCommonGet("client", db::getClientByPass);

        get("/advocate", (request, response) -> {
            Advocate advocate = authAdvocate(authUser(request));
            response.status(200);
            response.type("application/json");
            return gson.toJson(advocate);
        });

        get("/dashboard", (request, response) -> {
            Advocate advocate = authAdvocate(authUser(request));

            response.status(200);
            response.type("application/json");

            JsonObject jsonObject = new JsonObject();
            JsonArray agreementsJO = new JsonArray();
            JsonArray casesJO = new JsonArray();
            JsonArray receiptsJO = new JsonArray();

            List<Agreement> agreements = db.getAdvocateAgreements(advocate.getPassport(), 10);
            List<Case> cases = db.getAdvocateCases(advocate.getPassport(), 10);
            List<Receipt> receipts = db.getAdvocateReceipts(advocate.getPassport(), 10);

            for (Agreement agreement : agreements) {
                if (agreement.getClient() == 0) continue;
                Client client = db.getClientByPass(agreement.getClient());
                JsonObject agreementJO = new JsonObject();
                agreementJO.addProperty("number", agreement.getNumber());
                agreementJO.addProperty("client_name", client == null ? "UNKNOWN" : client.getName());
                agreementJO.addProperty("status", agreement.getStatus());
                agreementsJO.add(agreementJO);
            }

            for (Case c : cases) {
                JsonObject caseJO = new JsonObject();
                caseJO.addProperty("id", c.getId());
                caseJO.addProperty("case_name", c.getName());
                caseJO.addProperty("case_description", c.getDescription());
                caseJO.addProperty("status", c.getClosed_date() == null ? 0 : 1);
                casesJO.add(caseJO);
            }

            for (Receipt receipt : receipts) {
                Client client = db.getClientByPass(receipt.getClient());
                JsonObject receiptJO = new JsonObject();
                receiptJO.addProperty("id", receipt.getId());
                receiptJO.addProperty("client_name", client == null ? "UNKNOWN" : client.getName());
                receiptJO.addProperty("amount", receipt.getAmount());
                receiptJO.addProperty("status", receipt.getStatus());
                receiptsJO.add(receiptJO);
            }

            jsonObject.add("agreements", agreementsJO);
            jsonObject.add("cases", casesJO);
            jsonObject.add("receipts", receiptsJO);

            return gson.toJson(jsonObject);
        });
    }

    private <T> void initCommonGet(String name, Function<Integer, T> function) {
        get("/%s/:id".formatted(name), (request, response) -> {
            authAdvocate(authUser(request));

            int id = parseInt(request.params(":id"));
            T obj = function.apply(id);
            if (obj == null)
                halt(404);

            response.status(200);
            response.type("application/json");
            return gson.toJson(obj);
        });
    }

    private @NotNull Long authUser(Request request) {
        String accessCode = request.cookie("code");

        Long userId = auth.auth(accessCode);
        if (userId == null) halt(401);
        return userId;
    }

    private @NotNull Advocate authAdvocate(Long userId) {
        Advocate advocate = db.getAdvocateByDiscord(userId);
        if (advocate == null) halt(403);
        return advocate;
    }

    private @NotNull Integer parseInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            halt(400);
        }
        return null;
    }

    public void close() {
        stop();
    }

}
