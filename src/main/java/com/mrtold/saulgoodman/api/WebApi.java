package com.mrtold.saulgoodman.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.discord.DsUtils;
import com.mrtold.saulgoodman.logic.model.*;
import com.mrtold.saulgoodman.services.Authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

import static com.mrtold.saulgoodman.discord.DsUtils.hasNotHighPermission;
import static spark.Spark.*;

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

        post("/api/claim", (req, res) -> {
            Advocate requester = getAdvocate(req);

            JsonObject data = gson.fromJson(req.body(), JsonObject.class);
            
            log.info("ADV: {} calls for POST: /claim", requester.getName());
            
            Claim claim = new Claim(
                    data.get("description") == null ? null : data.get("description").getAsString(),
                    data.get("type").getAsString(),
                    data.get("number") == null ? null :data.get("number").getAsInt(),
                    data.get("status").getAsInt(),
                    new Date(data.get("happened").getAsLong())
            );
            HashSet<Advocate> advocates = new HashSet<Advocate>();
            advocates.add(requester);
            claim.setAdvocates(advocates);
            db.saveClaim(claim);

            return gson.toJson(claim.getId());
        });

        get("/api/claims", (req, res) -> {
            Advocate requester = getAdvocate(req);
            List<Claim> claims = null;
            log.info("ADV: {} calls for GET: /claims", requester.getName());
            if (hasNotHighPermission(requester.getDsUserId())) {
                claims = db.getAPIClaims(requester.getPassport());
            } else {
                claims = db.getAPIClaims(0);
            }

            if (claims == null) {
                res.status(500);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Failed to retrieve claims");
                return gson.toJson(error);
            }
        
            // Create a JSON array to hold the claims
            JsonArray jsonArray = new JsonArray();
        
            // Process each claim
            for (Claim claim : claims) {
                JsonObject clientJson = new JsonObject();
                claim.getClients().stream().findFirst().ifPresent(client -> {
                    // Populate JSON object for client
                    clientJson.addProperty("name", client.getName());
                    clientJson.addProperty("passport", client.getPassport());
                });

                // Create JSON object for the claim
                JsonObject claimJson = new JsonObject();
                claimJson.addProperty("id", claim.getId());
                claimJson.addProperty("description", claim.getDescription());
                claimJson.addProperty("type", claim.getType());
                claimJson.addProperty("number", claim.getNumber());
                claimJson.addProperty("status", claim.getStatus());
                claimJson.addProperty("side", claim.getSide());
                claimJson.addProperty("happened", claim.getHappened().toString());
                claimJson.add("client", clientJson);
        
                // Add the claim to the JSON array
                jsonArray.add(claimJson);
            }
        
            // Return the JSON array as the response
            return gson.toJson(jsonArray);
        });
        get("/api/claim/:id", (req, res) -> {
            Advocate requester = getAdvocate(req);
            Long id = Long.parseLong(req.params(":id"));
            
            log.info("ADV: {} calls for GET: /claim/{}", requester.getName(), id);

            // if executive - allowed, otherwise - permitted
            if (hasNotHighPermission(requester.getDsUserId())) {
                // if requester permitted to get claim
                if (db.getAdvocateCases(requester.getPassport(), 99999).contains(id)) {
                    res.status(406);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Advocate is not allowed to perform this action");
                    return gson.toJson(error);
                }
            }

            // Fetching the data from the database
            Claim claim = db.getAPIClaim(id);
            if (claim == null) {
                res.status(404);
                JsonObject error = new JsonObject();
                error.addProperty("error", "The claim is not found");
                return gson.toJson(error);
            }
            // Create the main JSON object for the claim
            JsonObject claimJson = new JsonObject();
            claimJson.addProperty("id", claim.getId());
            claimJson.addProperty("number", claim.getNumber());
            claimJson.addProperty("type", claim.getType());
            claimJson.addProperty("description", claim.getDescription());
            claimJson.addProperty("happened", claim.getHappened().toString());
            claimJson.addProperty("sent", claim.getSent() != null ? claim.getSent().toString() : null);
            claimJson.addProperty("hearing", claim.getHearing() != null ? claim.getHearing().toString() : null);
            claimJson.addProperty("status", claim.getStatus());
            claimJson.addProperty("side", claim.getSide());
            claimJson.addProperty("header", claim.getHeader());
            claimJson.addProperty("forumLink", claim.getForumLink());
            claimJson.addProperty("paymentLink", claim.getPaymentLink());

            // Creating the clients array
            JsonArray clientsArray = new JsonArray();
            for (Client client : claim.getClients()) {
                JsonObject clientJson = new JsonObject();
                clientJson.addProperty("passport", client.getPassport());
                clientJson.addProperty("name", client.getName());
                clientJson.addProperty("phone", client.getPhone());
                clientJson.addProperty("discordName", DsUtils.getDiscordName(client.getDsUserId()));
                clientJson.addProperty("passportLink", client.getPassportLink());
                clientJson.addProperty("agreementNumber", client.getAgreement());
                clientJson.addProperty("agreementLink", client.getAgreementLink());
                clientsArray.add(clientJson);
            }
            claimJson.add("clients", clientsArray);

            // Creating the advocates array
            JsonArray advocatesArray = new JsonArray();
            for (Advocate advocate : claim.getAdvocates()) {
                JsonObject advocateJson = new JsonObject();
                advocateJson.addProperty("passport", advocate.getPassport());
                advocateJson.addProperty("name", advocate.getName());
                advocateJson.addProperty("phone", advocate.getPhone());
                advocateJson.addProperty("discordName", DsUtils.getDiscordName(advocate.getDsUserId()));
                advocateJson.addProperty("licenseLink", advocate.getLicenseLink());
                advocateJson.addProperty("passportLink", advocate.getPassLink());
                advocateJson.addProperty("signatureLink", advocate.getSignatureLink());
                advocatesArray.add(advocateJson);
            }
            claimJson.add("advocates", advocatesArray);

            // Creating the evidences array
            JsonArray evidencesArray = new JsonArray();
            for (Evidence evidence : claim.getEvidences()) {
                JsonObject evidenceJson = new JsonObject();
                evidenceJson.addProperty("id", evidence.getId());
                evidenceJson.addProperty("name", evidence.getName());
                evidenceJson.addProperty("link", evidence.getLink());
                evidenceJson.addProperty("obtaining", evidence.getObtaining());
                evidencesArray.add(evidenceJson);
            }
            claimJson.add("evidences", evidencesArray);

            // Set the response type and body
            res.type("application/json");
            return claimJson.toString();
        });
        post("/api/claim/:id", (req, res) -> {
            Advocate advocate = getAdvocate(req);
            Long id = Long.parseLong(req.params(":id"));
            
            log.info("ADV: {} calls for POST: /claim/{}", advocate.getName(), id);

            JsonObject data = gson.fromJson(req.body(), JsonObject.class);
            
            // if executive - allowed, otherwise - permitted
            if (hasNotHighPermission(advocate.getDsUserId())) {
                // if advocate Permitted to get claim
                if (db.getAdvocateCases(advocate.getPassport(), 99999).contains(id)) {
                    res.status(406);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Advocate is not allowed to perform this action");
                    return gson.toJson(error);
                }
            }

            // Fetching the data from the database
            Claim claim = db.getAPIClaim(id);
            if (claim == null) {
                res.status(404);
                JsonObject error = new JsonObject();
                error.addProperty("error", "The claim is not found");
                return gson.toJson(error);
            }

            synchronized (claim) {
                for (Map.Entry<String, JsonElement> element : data.entrySet()) {
                    switch (element.getKey()) {
                        case "number" -> claim.setNumber(element.getValue().getAsInt());
                        case "type" -> claim.setType(element.getValue().getAsString());
                        case "description" -> claim.setDescription(element.getValue().getAsString());
                        case "happened" -> claim.setHappened(new Date(element.getValue().getAsLong()));
                        case "sent" -> claim.setSent(new Date(element.getValue().getAsLong()));
                        case "hearing" -> claim.setHearing(new Date(element.getValue().getAsLong()));
                        case "status" -> claim.setStatus(element.getValue().getAsInt());
                        case "side" -> claim.setSide(element.getValue().getAsInt());
                        case "header" -> claim.setHeader(element.getValue().getAsString());
                        case "forumLink" -> claim.setForumLink(element.getValue().getAsString());
                        case "paymentLink" -> claim.setPaymentLink(element.getValue().getAsString());
                    }
                }

                db.saveClaim(claim);
            }
            return 200;
        });

        get("/api/advocates/", (req, res) -> {
            getAdvocate(req);
            return gson.toJson(db.getAllAdvocatesShort());
        });
        post("/api/claim/:id/advocate/:passport", (req, res) -> {
            Advocate requester = getAdvocate(req);
            Long id = Long.parseLong(req.params(":id"));
            Integer passport = Integer.parseInt(req.params(":passport"));

            Claim claim = db.getClaimById(id);
            Advocate advocate = db.getAdvocateByPass(passport);
            
            log.info("ADV: {} calls for POST: /claim/{}/advocate/{}", requester.getName(), id, passport);

            if (claim == null || advocate == null) {
                return 404;
            }
            
            claim.getAdvocates().add(advocate);
            db.saveClaim(claim);

            return 200;
        });
        post("/api/claim/:id/advocate/:passport/remove", (req, res) -> {
            Advocate requester = getAdvocate(req);
            Long id = Long.parseLong(req.params(":id"));
            Integer passport = Integer.parseInt(req.params(":passport"));
        
            Claim claim = db.getClaimById(id);
            Advocate advocate = db.getAdvocateByPass(passport);
        
            log.info("ADV: {} calls for POST: /claim/{}/advocate/{}/remove", requester.getName(), id, passport);
        
            if (claim == null || advocate == null) {
                res.status(404);
                return "Not Found";
            }
            
            Set<Advocate> advocates = claim.getAdvocates();

            for (Advocate adv : advocates) {

                if (adv.getPassport() == advocate.getPassport()) {
                    advocates.remove(adv);
                }
            };

            claim.setAdvocates(advocates);
            db.saveClaim(claim);

            return 200;
        });
        post("/api/advocate/:id", (req, res) -> {
            Advocate requester = getAdvocate(req);

            int id = Integer.parseInt(req.params(":id"));
            Advocate advocate = db.getAdvocateByPass(id);
            if (advocate == null) halt(404);

            log.info("ADV: {} calls for POST: /advocate/{}", requester.getName(), id);

            JsonObject data = gson.fromJson(req.body(), JsonObject.class);

            synchronized (advocate) {
                for (Map.Entry<String, JsonElement> element : data.entrySet()) {
                    switch (element.getKey()) {
                        case "phone" -> advocate.setPhone(element.getValue().getAsInt());
                        case "passportLink" -> advocate.setPassLink(element.getValue().getAsString());
                        case "licenseLink" -> advocate.setLicenseLink(element.getValue().getAsString());
                        case "signatureLink" -> advocate.setSignatureLink(element.getValue().getAsString());
                    }
                }

                db.saveAdvocate(advocate);
            }

            return 200;
        });

        get("/api/clients/", (req, res) -> {
            getAdvocate(req);
            return gson.toJson(db.getAllClientsShort());
        });
        post("/api/claim/:id/client/:passport", (req, res) -> {
            Advocate requester = getAdvocate(req);
            Long id = Long.parseLong(req.params(":id"));
            Integer passport = Integer.parseInt(req.params(":passport"));

            Claim claim = db.getClaimById(id);
            Client client = db.getClientByPass(passport);

            log.info("ADV: {} calls for POST: /claim/{}/client/{}", requester.getName(), id, passport);

            if (claim == null || client == null) {
                return 404;
            }

            claim.getClients().add(client);
            db.saveClaim(claim);

            return 200;
        });
        post("/api/claim/:id/client/:passport/remove", (req, res) -> {
            Advocate requester = getAdvocate(req);
            Long id = Long.parseLong(req.params(":id"));
            Integer passport = Integer.parseInt(req.params(":passport"));
        
            Claim claim = db.getClaimById(id);
            Client client = db.getClientByPass(passport);
            
            log.info("ADV: {} calls for POST: /claim/{}/client/{}/remove", requester.getName(), id, passport);
        
            if (claim == null || client == null) {
                res.status(404);
                return "Not Found";
            }

            Set<Client> clients = claim.getClients();

            for (Client cli : clients) {
                if (cli.getPassport() == client.getPassport()) {
                    clients.remove(cli);
                }
            };

            db.saveClaim(claim);
        
            return 200;
        });
        post("/api/client/:id", (req, res) -> {
            Advocate requester = getAdvocate(req);

            int id = Integer.parseInt(req.params(":id"));
            Client client = db.getClientByPass(id);
            if (client == null) halt(404);

            log.info("ADV: {} calls for POST: /client/{}", requester.getName(), id);

            JsonObject data = gson.fromJson(req.body(), JsonObject.class);

            synchronized (client) {
                for (Map.Entry<String, JsonElement> element : data.entrySet()) {
                    switch (element.getKey()) {
                        case "phone" -> client.setPhone(element.getValue().getAsInt());
                        case "passportLink" -> client.setPassportLink(element.getValue().getAsString());
                        case "agreementLink" -> client.setAgreementLink(element.getValue().getAsString());
                    }
                }

                db.saveClient(client);
            }

            return 200;
        });

        post("/api/claim/:id/evidence/", (req, res) -> {
            Advocate requester = getAdvocate(req);
            Long id = Long.parseLong(req.params(":id"));
            Claim claim = db.getClaimById(id);

            log.info("ADV: {} calls for POST: /claim/{}/evidence", requester.getName(), id);

            JsonObject data = gson.fromJson(req.body(), JsonObject.class);

            Evidence evidence = new Evidence(
                    data.get("name").getAsString(),
                    data.get("link").getAsString(),
                    data.get("obtaining") == null ? null :data.get("obtaining").getAsString(),
                    claim,
                    requester
            );
            evidence = db.saveEvidence(evidence);
            claim.addEvidence(evidence);
            db.saveClaim(claim);

            return gson.toJson(evidence.getId());
        });
        post("/api/claim/:id/evidence/:idE/remove", (req, res) -> {
            Advocate requester = getAdvocate(req);
            Long id = Long.parseLong(req.params(":id"));
            Long idE = Long.parseLong(req.params(":idE"));
            
            log.info("ADV: {} calls for POST: /claim/{}/evidence/{}/remove", requester.getName(), id, idE);

            Claim claim = db.getClaimById(id);
            claim.getEvidences().remove(db.getEvidenceById(idE));

            return 200;
        });
        post("/api/evidence/:id", (req, res) -> {
            Advocate requester = getAdvocate(req);

            int id = Integer.parseInt(req.params(":id"));
            Evidence evidence = db.getEvidenceById(id);
            if (evidence == null) halt(404);

            log.info("ADV: {} calls for POST: /evidence/{}", requester.getName(), id);

            JsonObject data = gson.fromJson(req.body(), JsonObject.class);

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
