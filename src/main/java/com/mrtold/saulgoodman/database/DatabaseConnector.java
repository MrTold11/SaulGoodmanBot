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

    public @Nullable Client getClientByPass(@Nullable Integer passport) {
        if (passport == null) return null;
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Client C where C.passport = :pass", Client.class)
                    .setParameter("pass", passport).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public @Nullable Client getClientByDiscord(@Nullable Long dsId) {
        if (dsId == null) return null;
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Client C where C.dsUserId = :dsId",
                    Client.class).setParameter("dsId", dsId).getSingleResult();
        } catch (Exception e) {
            return null;
        }
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

    public @Nullable Advocate getAdvocateByDiscord(long dsId) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Advocate A where A.dsUserId = :dsId", Advocate.class)
                    .setParameter("dsId", dsId).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public @Nullable Advocate getAdvocateByPass(int pass) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Advocate A where A.passport = :pass", Advocate.class)
                    .setParameter("pass", pass).getSingleResult();
        } catch (Exception e) {
            return null;
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

    public @Nullable Agreement getAgreementById(long id) {
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

    public @Nullable Receipt getReceipt(int id) {
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
        return client;
    }

    public void saveAdvocate(@NotNull Advocate advocate) {
        sessionFactory.inTransaction(session -> session.merge(advocate));
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
    }

    public void close() {
        sessionFactory.close();
    }

}
