package com.mrtold.saulgoodman.database;

import com.mrtold.saulgoodman.logic.model.*;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mr_Told
 */
public class DatabaseConnector {

    static DatabaseConnector instance;

    public static DatabaseConnector getInstance() {
        if (instance == null)
            throw new IllegalStateException("Database Connector not initialized");
        return instance;
    }

    public static DatabaseConnector init(String ip, int port, String database, String user, String pass) {
        instance = new DatabaseConnector(ip, port, database, user, pass);
        return instance;
    }

    final Logger log;
    final SessionFactory sessionFactory;
    final Map<Integer, Client> clientsByPass = new ConcurrentHashMap<>();
    final Map<Long, Client> clientsByDiscord = new ConcurrentHashMap<>();
    final Map<Integer, Advocate> advocateByPass = new ConcurrentHashMap<>();
    final Map<Long, Advocate> advocateByDiscord = new ConcurrentHashMap<>();

    DatabaseConnector(String ip, int port, String database, String user, String pass) {
        log = LoggerFactory.getLogger(DatabaseConnector.class);

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.url",
                        String.format(Locale.getDefault(), "jdbc:postgresql://%s:%d/%s", ip, port, database))
                .applySetting("hibernate.connection.username", user)
                .applySetting("hibernate.connection.password", pass)
                .applySetting("hibernate.hbm2ddl.auto", "update")
                .applySetting("hibernate.connection.autoReconnect", "true")
                .applySetting("hibernate.connection.provider_class",
                        "org.hibernate.hikaricp.internal.HikariCPConnectionProvider")
                .build();

        try {
            sessionFactory =
                    new MetadataSources(registry)
                            .addAnnotatedClass(Client.class)
                            .addAnnotatedClass(Advocate.class)
                            .addAnnotatedClass(Agreement.class)
                            .addAnnotatedClass(Claim.class)
                            .addAnnotatedClass(Evidence.class)
                            .addAnnotatedClass(Receipt.class)
                            .buildMetadata()
                            .buildSessionFactory();
        } catch (Exception e) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw e;
        }

        init();
    }

    private void init() {
        sessionFactory.inTransaction(session -> session.merge(
                new Client(0, 0L, "System Client", null)));
    }

    private void cacheClient(Client client) {
        clientsByPass.put(client.getPassport(), client);
        clientsByDiscord.put(client.getDsUserId(), client);
    }

    private void cacheAdvocate(Advocate advocate) {
        advocateByPass.put(advocate.getPassport(), advocate);
        advocateByDiscord.put(advocate.getDsUserId(), advocate);
    }

    public @Nullable Client getClientByPass(@Nullable Integer passport) {
        if (passport == null) return null;

        Client client = clientsByPass.get(passport);
        if (client != null) return client;

        try (Session session = sessionFactory.openSession()) {
            client = session.createQuery("from Client C where C.passport = :pass", Client.class)
                    .setParameter("pass", passport).getSingleResult();
            cacheClient(client);
        } catch (Exception ignored) {
        }
        return client;
    }

    public List<Client> getClientTwinks(@Nullable Long dsId) {
        if (dsId == null) return Collections.emptyList();

        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Client C where C.dsUserId = :dsId",
                    Client.class).setParameter("dsId", dsId).getResultList();
        }
    }

    public @Nullable Client getClientByDiscord(@Nullable Long dsId) {
        if (dsId == null) return null;

        Client client = clientsByDiscord.get(dsId);
        if (client != null) return client;

        try (Session session = sessionFactory.openSession()) {
            client = session.createQuery("from Client C where C.dsUserId = :dsId",
                    Client.class).setParameter("dsId", dsId).getSingleResult();
            cacheClient(client);
        } catch (Exception ignored) {
        }
        return client;
    }

    public @Nullable Client getClientByChannel(@Nullable Long channelId) {
        if (channelId == null) return null;

        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Client C where C.dsUserChannel = :dsId",
                    Client.class).setParameter("dsId", channelId).getSingleResult();
        } catch (Exception ignored) {
        }
        return null;
    }

    @Nullable
    public Client getClient(@Nullable Long dsId, Integer pass) {
        Client client = getClientByPass(pass);
        if (client != null)
            return client;

        return getClientByDiscord(dsId);
    }

    public @NotNull Client getOrCreateClient(Integer passport, long dsId, String name) {
        Client client = getClientByPass(passport);
        if (client == null)
            return saveClient(new Client(passport, dsId, name, null));
        return client;
    }

    public @Nullable Advocate getAdvocateByDiscord(Long dsId) {
        if (dsId == null) return null;

        Advocate advocate = advocateByDiscord.get(dsId);
        if (advocate != null) return advocate;

        try (Session session = sessionFactory.openSession()) {
            advocate = session.createQuery("from Advocate A where A.dsUserId = :dsId", Advocate.class)
                    .setParameter("dsId", dsId).getSingleResult();
            cacheAdvocate(advocate);
        } catch (Exception ignored) {
        }
        return advocate;
    }

    public @Nullable Advocate getAdvocateByPass(Integer pass) {
        if (pass == null) return null;

        Advocate advocate = advocateByPass.get(pass);
        if (advocate != null) return advocate;

        try (Session session = sessionFactory.openSession()) {
            advocate = session.createQuery("from Advocate A where A.passport = :pass", Advocate.class)
                    .setParameter("pass", pass).getSingleResult();
            cacheAdvocate(advocate);
        } catch (Exception ignored) {
        }
        return advocate;
    }

    public @NotNull List<Agreement> getAdvocateAgreements(int pass, int limit) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            "from Agreement A where A.advocate = :pass order by A.id desc", Agreement.class)
                    .setMaxResults(limit)
                    .setParameter("pass", pass).getResultList();
        }
    }

    @Deprecated
    public @NotNull List<Claim> getAllClaimsShort() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                    "select new com.mrtold.saulgoodman.logic.model.Claim(C.id, C.description, C.type, C.number, C.status, C.happened)" +
                            " from Claim C order by C.id desc", Claim.class)
                    .getResultList();
        }
    }

    
    // API CLAIMS
    public @NotNull List<Claim> getAPIClaims(int advocateID) {
        String hql_query;

        if (advocateID > 0) {
            // Correct query with proper syntax and ordering
            hql_query = "SELECT new com.mrtold.saulgoodman.logic.model.Claim(C.id, C.type, C.number, C.description, C.happened, C.status, C.side) FROM Claim C WHERE :advocateID IN ELEMENTS(C.advocates) ORDER BY C.id DESC";
        } else {
            // Query without advocate filtering
            hql_query = "SELECT new com.mrtold.saulgoodman.logic.model.Claim(C.id, C.type, C.number, C.description, C.happened, C.status, C.side) FROM Claim C ORDER BY C.id DESC";
        }

        try (Session session = sessionFactory.openSession()) {
            Query<Claim> query = session.createQuery(hql_query, Claim.class);

            if (advocateID > 0) {
                // Set parameter for the query
                query.setParameter("advocateID", advocateID);
            }

            List<Claim> results = query.getResultList();
            log.debug("Fetched claims: {}", results);

            return results;
        } catch (Exception e) {
            log.error("Exception at (DB) GET: /api/claims", e);
            return null;
        }
    }
    public @NotNull Claim getAPIClaim(long claimID) {
        String hql_query = "SELECT new com.mrtold.saulgoodman.logic.model.Claim(C.id, C.description, C.type, C.number, C.status, C.side, C.happened, C.sent, C.hearing, C.header, C.forumLink, C.paymentLink) FROM Claim C WHERE C.id = :claimId";
        
        try (Session session = sessionFactory.openSession()) {
            Query<Claim> query = session.createQuery(hql_query, Claim.class).setParameter("claimId", claimID);

            return query.getSingleResult();
        } catch (Exception e) {
            log.error("Exception at (DB) GET: /api/claim/:id", e);
            return null;
        }
    }
    
    @SuppressWarnings("deprecation")
    public @NotNull List<Long> getPermittedClaims(int advocatePassport) {
        String sql_query = "WITH all_advocates AS (SELECT DISTINCT CA.claim_id, A.name AS advocateName, A.passport AS advocatePassport FROM advocate A INNER JOIN claim_advocate CA ON A.passport = CA.advocates_passport ) select DISTINCT C.id FROM claim C LEFT JOIN all_advocates AA ON C.id = AA.claim_id WHERE AA.advocatePassport = :advocatePassport";
        try (Session session = sessionFactory.openSession()) {
            Query query = session.createNativeQuery(sql_query);
            
            if (advocatePassport > 0){
                query.setParameter("advocatePassport", advocatePassport);
            }

            // Fetching the result
            List<Object[]> results = query.getResultList();
            log.debug(results.toString());
            List<Long> claimIDs = new ArrayList<>();

            // Transforming result's Objects into Long (claim's ID data type)
            for (Object[] row : results) {
                claimIDs.add((Long) row[0]);
            }
            log.debug(claimIDs.toString());

            return claimIDs;
        } catch (Exception e) {
            log.error("Exception at (DB) getting permitted_claims", e);
            return null;
        }
    }

    // API EVIDENCES
    public @NotNull List<Evidence> getAPIClaimEvidences(long claimID) {
        String hql_query = "SELECT new com.mrtold.saulgoodman.logic.model.Evidence(E.id, E.name, E.link, E.obtaining) FROM Evidence E WHERE E.claim = :claimID";

        try (Session session = sessionFactory.openSession()) {
            List<Evidence> results = session.createQuery(hql_query, Evidence.class).setParameter("claimID", claimID).getResultList();
            log.debug(results.toString());
            return results;
        } catch (Exception e) {
            log.error("Exception at (DB) getting claim's evidence", e);
            return null;
        }
    }
    
    // API CLIENTS
    public @NotNull List<Client> getAPIClaimClients(long claimID) {
        String hql_query = "SELECT new com.mrtold.saulgoodman.logic.model.Client(C.passport, C.name, C.dsUserId, C.phone) FROM Client C JOIN C.clients CC ON C.passport = CC.clientsPassport WHERE CC.claimId = :claimID";

        try (Session session = sessionFactory.openSession()) {
            List<Client> results = session.createQuery(hql_query, Client.class).setParameter("claimID", claimID).getResultList();
            log.debug(results.toString());
            return results;
        } catch (Exception e) {
            log.error("Exception at (DB) getting claim's clients", e);
            return null;
        }
    }
    public @NotNull Client getAPIClaimClient(long claimID) {
        String hql_query = "SELECT new com.mrtold.saulgoodman.logic.model.Client(C.passport, C.name) FROM Client C JOIN Claim.clients CC ON C.passport = CC.clientsPassport WHERE CC.claimId = :claimID";

        try (Session session = sessionFactory.openSession()) {
            Client result = session.createQuery(hql_query, Client.class).setParameter("claimID", claimID).setMaxResults(1).getSingleResult();
            log.debug(result.toString());
            return result;
        } catch (Exception e) {
            log.error("Exception at (DB) getting claim's single client", e);
            return null;
        }
    }
    
    // API ADVOCATES
    public @NotNull List<Advocate> getAPIClaimAdvocates(long claimID) {
        String hql_query = "SELECT new com.mrtold.saulgoodman.logic.model.Advocate(A.passport, A.name, A.dsUserId, A.phone) FROM Advocate A JOIN C.advocates CA ON A.passport = CA.advocatesPassport WHERE CA.claimId = :claimID";

        try (Session session = sessionFactory.openSession()) {
            List<Advocate> results = session.createQuery(hql_query, Advocate.class).setParameter("claimID", claimID).getResultList();
            log.debug(results.toString());
            return results;
        } catch (Exception e) {
            log.error("Exception at (DB) getting claim's advocates", e);
            return null;
        }
    }
    public @NotNull Advocate getAPIClaimAdvocate(long claimID) {
        String hql_query = "SELECT new com.mrtold.saulgoodman.logic.model.Advocate(A.passport, A.name) FROM Advocate A JOIN Claim.advocates CA ON A.passport = CA.advocatesPassport WHERE CA.claimId = :claimID";

        try (Session session = sessionFactory.openSession()) {
            Advocate result = session.createQuery(hql_query, Advocate.class).setParameter("claimID", claimID).setMaxResults(1).getSingleResult();
            log.debug(result.toString());
            return result;
        } catch (Exception e) {
            log.error("Exception at (DB) getting claim's single advocate", e);
            return null;
        }
    }
    

    public Claim getClaimById(long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Claim C where C.id = :id", Claim.class)
                    .setParameter("id", id).getSingleResult();
        } catch (Exception e) {
            log.error(null, e);
            return null;
        }
    }

    public Evidence getEvidenceById(long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Evidence E where E.id = :id", Evidence.class)
                    .setParameter("id", id).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public @NotNull List<Claim> getAdvocateCases(int pass, int limit) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            "select C from Claim C where :pass in elements(C.advocates) order by C.id desc",
                            Claim.class)
                    .setMaxResults(limit)
                    .setParameter("pass", pass).getResultList();
        }
    }

    public @NotNull List<Receipt> getAdvocateReceipts(int pass, int limit) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Receipt R where R.author = :pass order by R.id desc",
                            Receipt.class)
                    .setMaxResults(limit)
                    .setParameter("pass", pass).getResultList();
        }
    }

    public @NotNull List<Agreement> getActiveAgreements() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Agreement A where A.status = 1 order by A.id asc",
                    Agreement.class).getResultList();
        }
    }

    public @NotNull List<Receipt> getActiveReceipts() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Receipt R where R.status = 0 order by R.id asc",
                    Receipt.class).getResultList();
        }
    }

    public @Nullable Agreement getAgreementById(int id) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Agreement A where A.id = :aId",
                    Agreement.class).setParameter("aId", id).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean clientHasActiveAgreement(int pass) {
        try (Session session = sessionFactory.openSession()) {
            session.createQuery("from Agreement A where A.client = :cPass and A.status = 1",
                    Agreement.class).setParameter("cPass", pass).getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        } catch (NonUniqueResultException e) {
            return true;
        }
    }

    public @Nullable Agreement getActiveAgreement(int clientPass) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Agreement A where A.client = :cPass and A.status = 1",
                    Agreement.class).setParameter("cPass", clientPass).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public @Nullable Receipt getReceipt(Integer id) {
        if (id == null) return null;
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Receipt R where R.id = :rid",
                    Receipt.class).setParameter("rid", id).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public @NotNull List<Client> getAllClients() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Client", Client.class).getResultList();
        }
    }
    public @NotNull List<Client> getAllClientsShort() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("select new com.mrtold.saulgoodman.logic.model.Client(C.passport, C.name)" +
            " from Client C", Client.class).getResultList();
        }
    }
    public @NotNull List<Advocate> getAllAdvocates() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Advocate", Advocate.class).getResultList();
        }
    }
    public @NotNull List<Advocate> getAllAdvocatesShort() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("select new com.mrtold.saulgoodman.logic.model.Advocate(A.passport, A.name)" + 
            " from Advocate A", Advocate.class).getResultList();
        }
    }


    public @NotNull Client saveClient(@NotNull Client client) {
        sessionFactory.inTransaction(session -> session.merge(client));
        cacheClient(client);
        return client;
    }

    public void saveAdvocate(@NotNull Advocate advocate) {
        sessionFactory.inTransaction(session -> session.merge(advocate));
        cacheAdvocate(advocate);
    }

    public void saveAgreement(@NotNull Agreement agreement) {
        sessionFactory.inTransaction(session -> session.merge(agreement));
    }

    public Receipt saveReceipt(@NotNull Receipt receipt) {
        return sessionFactory.fromTransaction(session -> session.merge(receipt));
    }

    public void updateClient(@NotNull Client old, long dsId, String name, Long dsChannelId) {
        if (name != null) old.setName(name);
        old.setDsUserId(dsId);
        old.setDsUserChannel(dsChannelId);
        saveClient(old);
    }

    public void deleteClient(@NotNull Client client) {
        sessionFactory.inTransaction(session -> session.remove(client));
        clientsByPass.remove(client.getPassport());
        clientsByDiscord.remove(client.getDsUserId());
    }

    public void deleteAgreement(@NotNull Agreement agreement) {
        sessionFactory.inTransaction(session -> session.remove(agreement));
    }

    public void saveClaim(Claim claim) {
        sessionFactory.fromTransaction(session -> session.merge(claim));
    }

    public Evidence saveEvidence(Evidence evidence) {
        return sessionFactory.fromTransaction(session -> session.merge(evidence));
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    public List<Receipt> getReceipts(String parameter, Object value) {
        Session session = sessionFactory.openSession();
        List<Receipt> result;

        String queryBase = """
                SELECT receipt.id, receipt.issued, receipt.amount, receipt.status, advocate.name, client.name, client.dsuserchannel, receipt.ds_id\s
                                    FROM receipt\s
                                    INNER JOIN client ON receipt.client = client.passport INNER JOIN advocate ON receipt.author = advocate.passport
                """;

        switch (parameter) {
            case "ALL":
                result = session.createNativeQuery(queryBase).getResultList();

                log.debug("Result of an SQL Query at RECEIPT -> ALL : \n {}", result.toString());
                break;
            case "ADVOCATE":
                result = session.createNativeQuery(queryBase + "WHERE advocate.passport = :value").setParameter("value", value).getResultList();

                log.debug("Result of an SQL Query at RECEIPT -> ADVOCATE : \n {}", result.toString());
                break;
            case "DAYS":
                result = session.createNativeQuery(queryBase + "WHERE receipt.issued > current_date - interval ':value DAY'").setParameter("value", value).getResultList();

                log.debug("Result of an SQL Query at RECEIPT -> DAYS : \n {}", result.toString());
                break;
            default:
                result = null;
                break;
        }
        session.close();
        return result;
    }

    public void close() {
        sessionFactory.close();
    }

}
