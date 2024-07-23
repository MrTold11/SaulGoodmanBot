package com.mrtold.saulgoodman.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.logic.model.*;
import com.mrtold.saulgoodman.services.Authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

import java.util.*;
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

    @SuppressWarnings("SameParameterValue")
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
        initCommonGet("receipt", db::getReceipt, false);
        initCommonGet("agreement", db::getAgreementById, false);
        initCommonGet("advocate", db::getAdvocateByPass, false);
        initCommonGet("client", db::getClientByPass, false);
        initCommonGet("claim", db::getClaimById, true);
        initCommonGet("evidence", db::getEvidenceById, true);

        get("/authenticate", (request, response) -> {
            log.info("/api/authenticate REQUEST: {}", request.toString());

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

            List<Receipt> receipts = null;

            if (days != null)
                receipts = db.getReceipts("DAYS", days);
            else if (advocate != null)
                receipts = db.getReceipts("ADVOCATE", advocate);
            else if (hasNotHighPermission(user.getDsUserId()))
                receipts = db.getReceipts("ADVOCATE", user.getPassport());

            if (receipts == null)
                halt(404);

            return gson.toJson(receipts);
        });

        get("/claims", (request, response) -> {
            getAdvocate(request);
            return gson.toJson(db.getAllClaims());
        });

        post("/new_claim", (request, response) -> {
            getAdvocate(request);

            JsonObject data = gson.fromJson(request.body(), JsonObject.class);

            Claim claim = new Claim(
                    data.get("description") == null ? null : data.get("description").getAsString(),
                    data.get("type").getAsString(),
                    data.get("number") == null ? null :data.get("number").getAsInt(),
                    data.get("status").getAsInt(),
                    new Date(data.get("happened").getAsLong())
            );
            db.saveClaim(claim);

            return gson.toJson(claim.getId());
        });

        post("/edit_claim", (request, response) -> {
            getAdvocate(request);

            JsonObject data = gson.fromJson(request.body(), JsonObject.class);
            long id = data.get("id").getAsLong();
            Claim claim = db.getClaimById(id);

            if (claim == null) halt(404);

            synchronized (claim) {
                for (Map.Entry<String, JsonElement> element : data.entrySet()) {
                    switch (element.getKey()) {
                        case "description" -> claim.setDescription(element.getValue().getAsString());
                        case "type" -> claim.setType(element.getValue().getAsString());
                        case "number" -> claim.setNumber(element.getValue().getAsInt());
                        case "status" -> claim.setStatus(element.getValue().getAsInt());
                        case "side" -> claim.setSide(element.getValue().getAsInt());
                        case "happened" -> claim.setHappened(new Date(element.getValue().getAsLong()));
                        case "sent" -> claim.setSent(new Date(element.getValue().getAsLong()));
                        case "hearing" -> claim.setHearing(new Date(element.getValue().getAsLong()));
                        case "forum" -> claim.setForumLink(element.getValue().getAsString());
                        case "header" -> claim.setHeader(element.getValue().getAsString());
                        case "payment" -> claim.setPaymentLink(element.getValue().getAsString());
                        case "clients" -> {
                            Set<Client> clients = new HashSet<>();
                            for (JsonElement e : element.getValue().getAsJsonArray()) {
                                clients.add(db.getClientByPass(e.getAsInt()));
                            }
                            claim.setClients(clients);
                        }
                        case "lawyers" -> {
                            Set<Advocate> lawyers = new HashSet<>();
                            for (JsonElement e : element.getValue().getAsJsonArray()) {
                                lawyers.add(db.getAdvocateByPass(e.getAsInt()));
                            }
                            claim.setAdvocates(lawyers);
                        }
                        case "evidences" -> {
                            Set<Evidence> evidences = new HashSet<>();
                            for (JsonElement e : element.getValue().getAsJsonArray()) {
                                evidences.add(db.getEvidenceById(e.getAsInt()));
                            }
                            claim.setEvidences(evidences);
                        }
                    }
                }

                db.saveClaim(claim);
            }

            return 200;
        });

        post("/new_evidence", (request, response) -> {
            Advocate advocate = getAdvocate(request);

            JsonObject data = gson.fromJson(request.body(), JsonObject.class);

            Evidence evidence = new Evidence(
                    data.get("name").getAsString(),
                    data.get("link").getAsString(),
                    data.get("obtaining") == null ? null :data.get("obtaining").getAsString(),
                    db.getClaimById(data.get("id").getAsLong()),
                    advocate
            );
            db.saveEvidence(evidence);

            return gson.toJson(evidence.getId());
        });

        get("/advocate", (request, response) -> {
            Advocate user = getAdvocate(request);
            response.status(200);
            response.type("application/json");
            return gson.toJson(user);
        });
    }

    private <V, T> void initCommonGet(String name, Function<V, T> function, boolean isLong) {
        get("/%s/:id".formatted(name), (request, response) -> {
            getAdvocate(request);

            Object id;
            if (isLong)
                id = Long.parseLong(request.params(":id"));
            else
                id = Integer.parseInt(request.params(":id"));
            //noinspection unchecked
            T obj = function.apply((V) id);
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
            halt(401);
        }

        Advocate advocate = db.getAdvocateByDiscord(userId);

        if (advocate == null || advocate.isNotActive()) {
            if (advocate == null) log.warn("User is not advocate.");
            else log.warn("User is not ACTIVE advocate.");
            halt(403);
        }

        return advocate;
    }

    public void close() {
        stop();
    }

}
