package com.mrtold.saulgoodman.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.discord.DsUtils;
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

    public static WebApi init(String dsClientId, String dsClientSecret, String oAuth2Redirect, String keystorePath, String keystorePassword, int port) {
        instance = new WebApi(dsClientId, dsClientSecret, oAuth2Redirect, keystorePath, keystorePassword, port);
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

    WebApi(String dsClientId, String dsClientSecret, String oAuth2Redirect, String keystorePath, String keystorePassword, int port) {
        authentication = new Authentication(dsClientId, dsClientSecret, oAuth2Redirect);
        db = DatabaseConnector.getInstance();
        port(port);
        secure(keystorePath, keystorePassword, null, null);
        enableCORS("*", "*");
        init();
    }

    private void init() {
        initCommonGet("receipt", db::getReceipt, false);
        initCommonGet("agreement", db::getAgreementById, false);
        initCommonGet("claim", db::getClaimById, true);
        initCommonGet("evidence", db::getEvidenceById, true);

        get("/client/:id", (request, response) -> {
            getAdvocate(request);

            int id = Integer.parseInt(request.params(":id"));
            Client client = db.getClientByPass(id);
            if (client == null)
                halt(404);

            JsonObject data = gson.fromJson(gson.toJson(client), JsonObject.class);
            data.addProperty("discordName", DsUtils.getDiscordName(client.getDsUserId()));

            response.status(200);
            response.type("application/json");
            return gson.toJson(data);
        });

        get("/advocate/:id", (request, response) -> {
            getAdvocate(request);

            int id = Integer.parseInt(request.params(":id"));
            Advocate advocate = db.getAdvocateByPass(id);
            if (advocate == null)
                halt(404);

            JsonObject data = gson.fromJson(gson.toJson(advocate), JsonObject.class);
            data.addProperty("discordName", DsUtils.getDiscordName(advocate.getDsUserId()));

            response.status(200);
            response.type("application/json");
            return gson.toJson(data);
        });

        get("/authenticate", (request, response) -> {
            Advocate user = getAdvocate(request);
            JsonObject data = new JsonObject();

            data.addProperty("passport", user.getPassport());
            data.addProperty("name", user.getName());

            return data;
        });
        
        post("/edit/client/:id", (request, response) -> {
            getAdvocate(request);

            int id = Integer.parseInt(request.params(":id"));
            Client client = db.getClientByPass(id);
            if (client == null) halt(404);

            JsonObject data = gson.fromJson(request.body(), JsonObject.class);

            synchronized (client) {
                for (Map.Entry<String, JsonElement> element : data.entrySet()) {
                    switch (element.getKey()) {
                        case "phone" -> client.setPhone(element.getValue().getAsInt());
                        case "passportLink" -> client.setPassportLink(element.getValue().getAsString());
                    }
                }

                db.saveClient(client);
            }

            return 200;
        });

        post("/edit/advocate/:id", (request, response) -> {
            getAdvocate(request);

            int id = Integer.parseInt(request.params(":id"));
            Advocate advocate = db.getAdvocateByPass(id);
            if (advocate == null) halt(404);

            JsonObject data = gson.fromJson(request.body(), JsonObject.class);

            synchronized (advocate) {
                for (Map.Entry<String, JsonElement> element : data.entrySet()) {
                    switch (element.getKey()) {
                        case "phone" -> advocate.setPhone(element.getValue().getAsInt());
                        case "passportLink" -> advocate.setPassLink(element.getValue().getAsString());
                        case "licenseLink" -> advocate.setLicenseLink(element.getValue().getAsString());
                    }
                }

                db.saveAdvocate(advocate);
            }

            return 200;
        });
        post("/edit/evidence/:id", (request, response) -> {
            getAdvocate(request);

            int id = Integer.parseInt(request.params(":id"));
            Evidence evidence = db.getEvidenceById(id);
            if (evidence == null) halt(404);

            JsonObject data = gson.fromJson(request.body(), JsonObject.class);

            synchronized (evidence) {
                for (Map.Entry<String, JsonElement> element : data.entrySet()) {
                    switch (element.getKey()) {
                        case "name" -> evidence.setName(element.getValue().getAsString());
                        case "link" -> evidence.setLink(element.getValue().getAsString());
                        case "obtaining" -> evidence.setObtaining(element.getValue().getAsString());
                    }
                }

                db.saveEvidence(evidence);
            }

            return 200;
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
            return gson.toJson(db.getAllClaimsShort());
        });
        get("/clients/short", (request, response) -> {
            getAdvocate(request);
            return gson.toJson(db.getAllClientsShort());
        });
        get("/lawyers/short", (request, response) -> {
            getAdvocate(request);
            return gson.toJson(db.getAllAdvocatesShort());
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
                        case "forumLink" -> claim.setForumLink(element.getValue().getAsString());
                        case "header" -> claim.setHeader(element.getValue().getAsString());
                        case "paymentLink" -> claim.setPaymentLink(element.getValue().getAsString());
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
            Claim claim = db.getClaimById(data.get("id").getAsLong());

            Evidence evidence = new Evidence(
                    data.get("name").getAsString(),
                    data.get("link").getAsString(),
                    data.get("obtaining") == null ? null :data.get("obtaining").getAsString(),
                    claim,
                    advocate
            );
            evidence = db.saveEvidence(evidence);
            claim.addEvidence(evidence);
            db.saveClaim(claim);

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
