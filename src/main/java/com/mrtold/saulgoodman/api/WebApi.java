package com.mrtold.saulgoodman.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.logic.model.*;
import com.mrtold.saulgoodman.services.Authentication;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

import java.util.List;
import java.util.function.Function;

import static com.mrtold.saulgoodman.discord.DsUtils.hasNotHighPermission;
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

    private static void enableCORS(final String methods, final String headers) {
        options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null)
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null)
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", request.headers("origin"));
            response.header("Access-Control-Request-Method", methods);
            response.header("Access-Control-Allow-Headers", headers);
            response.header("Access-Control-Allow-Credentials", "true");
        });
    }

    final Logger log = LoggerFactory.getLogger(WebApi.class);
    final Gson gson = new Gson();
    final DatabaseConnector db;

    final Authentication authentication;

    WebApi(String dsClientId, String dsClientSecret, String oAuth2Redirect, int port) {
        authentication = new Authentication(dsClientId, dsClientSecret, oAuth2Redirect);
        db = DatabaseConnector.getInstance();
        port(port);
        enableCORS("*", "*");
        init();
    }

    private void init() {
        initCommonGet("receipt", db::getReceipt);
        initCommonGet("agreement", db::getAgreementById);
        initCommonGet("advocate", db::getAdvocateByPass);
        initCommonGet("client", db::getClientByPass);

        get("/authenticate", (request, response) -> {
            Advocate user = getAdvocate(request);
            JsonObject data = new JsonObject();

            data.addProperty("passport", user.getPassport());
            data.addProperty("name", user.getName());

            return data;
        });
        
        get("/receipts", (request, response) -> {
            Advocate user = getAdvocate(request);

            String days = request.queryParams("days");
            String advocate = request.queryParams("advocate");
            
            if (days != null) {
                return db.getReceipts("DAYS", days);
            }

            if (advocate != null) {
                return db.getReceipts("ADVOCATE", advocate);
            }
            

            if (hasNotHighPermission(user.getDsUserId())) {
                return db.getReceipts("ADVOCATE", user.getPassport());
            } else {
                return db.getReceipts("ALL", null);
            }
            
        });

        get("/cases", (request, response) -> {
            Advocate user = getAdvocate(request);

            String client = request.queryParams("client");
            String advocate = request.queryParams("advocate");
            String agreement = request.queryParams("agreement");

            
            if (client != null) {
                return db.getReceipts("CLIENT", client);
            }

            if (advocate != null) {
                return db.getReceipts("ADVOCATE", advocate);
            }
            if (agreement != null) {
                return db.getReceipts("AGREEMENT", agreement);
            }
            

            if (hasNotHighPermission(user.getDsUserId())) {
                return db.getReceipts("ADVOCATE", user.getPassport());
            } else {
                return db.getReceipts("ALL", null);
            }
            
        });

        get("/case/:id", (request, response) -> {
            Advocate user = getAdvocate(request);

            return db.getCaseDetails(Integer.parseInt(request.params("id")));
        });

        post("/case/open", (request, response) -> {
            Advocate user = getAdvocate(request);

            db.openCase(request.queryParams("name"), request.queryParams("desctiption"), user);

            return "Successfull!";
        });

        post("/case/close", (request, response) -> {
            Advocate user = getAdvocate(request);

            db.closeCase(Integer.parseInt(request.queryParams("caseID")), user, hasNotHighPermission(user.getDsUserId()));
            
            return "Successfull!";
        });

        get("/advocate", (request, response) -> {
            Advocate user = getAdvocate(request);
            response.status(200);
            response.type("application/json");
            return gson.toJson(user);
        });

        get("/dashboard", (request, response) -> {
            Advocate user =  getAdvocate(request);

            response.status(200);
            response.type("application/json");

            JsonObject jsonObject = new JsonObject();
            JsonArray agreementsJO = new JsonArray();
            JsonArray casesJO = new JsonArray();
            JsonArray receiptsJO = new JsonArray();

            List<Agreement> agreements = db.getAdvocateAgreements(user.getPassport(), 10);
            List<Case> cases = db.getAdvocateCases(user.getPassport(), 10);
            List<Receipt> receipts = db.getAdvocateReceipts(user.getPassport(), 10);

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
            getAdvocate(request);

            int id = parseInt(request.params(":id"));
            T obj = function.apply(id);
            if (obj == null)
                halt(404);

            response.status(200);
            response.type("application/json");
            return gson.toJson(obj);
        });
    }

    private Advocate getAdvocate(Request request) {
        Long userId = authentication.authenticate(request.cookie("code"));
        if (userId == null) {
            log.warn("Couldn't find discord id by access code.");
            halt(401);
        }

        Advocate advocate = db.getAdvocateByDiscord(userId);

        if (advocate == null || advocate.isNotActive()) {
            if (advocate == null) {
                log.warn("User is not advocate.");
            } else {
                log.warn("User is not ACTIVE advocate.");
            }
            halt(403);
        }

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
