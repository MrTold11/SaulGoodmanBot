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

import java.util.*;
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
                            .addAnnotatedClass(AgreementCases.class)
                            .addAnnotatedClass(Case.class)
                            .addAnnotatedClass(Receipt.class)
                            .buildMetadata()
                            .buildSessionFactory();
        }
        catch (Exception e) {
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
        if (client.getDsUserId() != null)
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
        } catch (Exception ignored) {}
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
        } catch (Exception ignored) {}
        return client;
    }

    public @Nullable Client getClientByChannel(@Nullable Long channelId) {
        if (channelId == null) return null;

        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Client C where C.dsUserChannel = :dsId",
                    Client.class).setParameter("dsId", channelId).getSingleResult();
        } catch (Exception ignored) {}
        return null;
    }

    @Nullable public Client getClient(@Nullable Long dsId, Integer pass) {
        Client client = getClientByPass(pass);
        if (client != null)
            return client;

        return getClientByDiscord(dsId);
    }

    public @NotNull Client getOrCreateClient(Integer passport, Long dsId, String name) {
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
        } catch (Exception ignored) {}
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
        } catch (Exception ignored) {}
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

    public @NotNull List<Case> getAdvocateCases(int pass, int limit) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                    "select C from Case C, AgreementCases AC, Agreement A " +
                            "where A.advocate = :pass and AC.agreement = A.id and C.id = AC.case_ " +
                            "order by C.id desc",
                            Case.class)
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
        if (client.getDsUserId() != null)
            clientsByDiscord.remove(client.getDsUserId());
    }

    public void deleteAgreement(@NotNull Agreement agreement) {
        sessionFactory.inTransaction(session -> session.remove(agreement));
    }


    @SuppressWarnings({ "deprecation", "unchecked" })
    public List<Receipt> getReceipts(String parameter, Object value) {
        Session session =  sessionFactory.getCurrentSession();
        List<Receipt> result = null;


        String queryBase = """
                    SELECT receipt.id, receipt.issued, receipt.amount, receipt.status, advocate.name, client.name, client.dsuserchannel, receipt.ds_id
                    FROM receipt 
                    INNER JOIN client ON receipt.client = client.passport INNER JOIN advocate ON receipt.author = advocate.passport
                """;

        switch (parameter) {
            case "ALL":
                
                result = session.createNativeQuery(queryBase).getResultList();
                
                log.debug("Result of an SQL Query at RECEIPT -> ALL : \n " + result.toString());

                return result;
            
            case "ADVOCATE":
                result = session.createNativeQuery(queryBase + "WHERE advocate.passport = :value").setParameter("value", value).getResultList();

                log.debug("Result of an SQL Query at RECEIPT -> ADVOCATE : \n " + result.toString());

                return result;

            case "DAYS":
                result = session.createNativeQuery(queryBase + "WHERE receipt.issued > current_date - interval ':value DAY'").setParameter("value", value).getResultList();

                log.debug("Result of an SQL Query at RECEIPT -> DAYS : \n " + result.toString());

                return result;
            default:
                return null;
        }
    }

    @SuppressWarnings({ "deprecation" })
    public void openCase(String name, String description, Advocate advocate) {
        Session session =  sessionFactory.getCurrentSession();
        session.createNativeQuery("""
                INSERT INTO case 
                (name, description, opened_date)
                VALUES 
                (:name, :description, current_date)
                """).setParameter("name", name).setParameter("description", description).executeUpdate();
        long caseID = session.createNativeQuery("""
                SELECT id FROM case WHERE name = :name
                """).setParameter("name", name).getFirstResult();
        session.createNativeQuery("""
                INSERT INTO agreements_cases 
                (agreeement, case)
                VALUES 
                (-:advocateID, :caseID)
                """).setParameter("advocateID", advocate.getPassport()).setParameter("caseID", caseID).executeUpdate();
        
    }

    @SuppressWarnings({ "deprecation" })
    public void closeCase(long caseID, Advocate advocate, boolean notAllowed) {
        Session session =  sessionFactory.getCurrentSession();
        try {
            long is_lawyer_can_close = session.createNativeQuery("""
                    SELECT id FROM agreements_cases WHERE agreement = -:advocateID, case = :caseID
                    """).setParameter("advocateID", advocate.getPassport()).setParameter("caseID", caseID).getResultCount();
            
            if (is_lawyer_can_close == 1) {
                            session.createNativeQuery("""
                                    UPDATE case
                                    SET closed_date = current_date
                                    WHERE id = :caseID
                                    """).setParameter("caseID", caseID);
            } else {
                if (notAllowed) {
                    throw new Exception("This lawyer is not in this case => Is not allowed to close it.");
                } else {
                    session.createNativeQuery("""
                                    UPDATE case
                                    SET closed_date = current_date
                                    WHERE id = :caseID
                                    """).setParameter("caseID", caseID);
                }
            }
        } catch (Exception e) {
            log.error("Exception during CLOSE CASE : ", e);
        }
    }

    @SuppressWarnings({ "deprecation" })
    public Case getCaseDetails(long caseID) {
        Session session = sessionFactory.getCurrentSession();
        
        return (Case) session.createNativeQuery("""
                    select "case".id, "case".opened_date, "case".closed_date, "case".name, "case".description, agreement.number, client.name, client.dsuserchannel, advocate.name
                    from agreements_cases
                    inner join agreement on agreements_cases.agreement = agreement.number
                    inner join client on agreement.client = client.passport
                    inner join advocate on agreement.advocate = advocate.passport
                    inner join "case" on agreements_cases."case" = "case".id
                    where "case".id = :caseID
                    """).getSingleResultOrNull();
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    public List<Case> getCases(String parameter, Object value) {
        Session session =  sessionFactory.getCurrentSession();
        List<Case> result = null;


        String queryBase = """
                    select "case".id, "case".opened_date, "case".closed_date, "case".name, agreement.number, client.name, client.dsuserchannel, advocate.name
                    from agreements_cases
                    inner join agreement on agreements_cases.agreement = agreement.number
                    inner join client on agreement.client = client.passport
                    inner join advocate on agreement.advocate = advocate.passport
                    inner join "case" on agreements_cases."case" = "case".id
                """;

        switch (parameter) {
            case "ALL":
                
                result = session.createNativeQuery(queryBase).getResultList();
                
                log.debug("Result of an SQL Query at CASE -> ALL : \n " + result.toString());

                return result;
            
            case "ADVOCATE":
                result = session.createNativeQuery(queryBase + "WHERE advocate.passport = :value").setParameter("value", value).getResultList();

                log.debug("Result of an SQL Query at CASE -> ADVOCATE : \n " + result.toString());

                return result;

            case "CLIENT":
                result = session.createNativeQuery(queryBase + "WHERE client.passport = :value").setParameter("value", value).getResultList();

                log.debug("Result of an SQL Query at CASE -> CLIENT : \n " + result.toString());

                return result;

            case "AGREEMENT":
                result = session.createNativeQuery(queryBase + "WHERE agreement.number = :value").setParameter("value", value).getResultList();

                log.debug("Result of an SQL Query at CASE -> AGREEMENT : \n " + result.toString());

                return result;
            default:
                return null;
        }
    }


    public void close() {
        sessionFactory.close();
    }

}
