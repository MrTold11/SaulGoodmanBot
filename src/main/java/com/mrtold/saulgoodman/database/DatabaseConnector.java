package com.mrtold.saulgoodman.database;

import com.mrtold.saulgoodman.logic.model.*;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public @NotNull List<Claim> getAllClaims() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                    "select new com.mrtold.saulgoodman.logic.model.Claim(C.id, C.description, C.type, C.number, C.status, C.happened)" +
                            " from Claim C order by C.id desc", Claim.class)
                    .getResultList();
        }
    }

    public Claim getClaimById(long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Claim C where C.id = :id", Claim.class)
                    .setParameter("id", id).getSingleResult();
        } catch (Exception e) {
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
        sessionFactory.inTransaction(session -> session.merge(claim));
    }

    public void saveEvidence(Evidence evidence) {
        sessionFactory.inTransaction(session -> session.merge(evidence));
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

                log.info("Result of an SQL Query at RECEIPT -> ALL : \n {}", result.toString());
                break;
            case "ADVOCATE":
                result = session.createNativeQuery(queryBase + "WHERE advocate.passport = :value").setParameter("value", value).getResultList();

                log.info("Result of an SQL Query at RECEIPT -> ADVOCATE : \n {}", result.toString());
                break;
            case "DAYS":
                result = session.createNativeQuery(queryBase + "WHERE receipt.issued > current_date - interval ':value DAY'").setParameter("value", value).getResultList();

                log.info("Result of an SQL Query at RECEIPT -> DAYS : \n {}", result.toString());
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
