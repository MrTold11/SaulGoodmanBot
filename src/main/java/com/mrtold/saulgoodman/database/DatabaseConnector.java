package com.mrtold.saulgoodman.database;

import com.mrtold.saulgoodman.model.Advocate;
import com.mrtold.saulgoodman.model.Client;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Mr_Told
 */
public class DatabaseConnector {

    final Logger log;
    final SessionFactory sessionFactory;
    final Map<Integer, Client> clientsByPass = new HashMap<>();
    final Map<Long, Client> clientsByDiscord = new HashMap<>();
    final Map<Long, Advocate> advocatesByDiscord = new HashMap<>();

    public DatabaseConnector(String ip, int port, String database, String user, String pass) {
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
                            .buildMetadata()
                            .buildSessionFactory();
            init();
        }
        catch (Exception e) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw e;
        }
    }

    private void init() {
        Session session = getSessionFactory().openSession();
        List<Client> clients = loadAllData(Client.class, session);
        for (Client client : clients) {
            addClient(client);
        }
        List<Advocate> advocates = loadAllData(Advocate.class, session);
        for (Advocate advocate : advocates) {
            if (advocate != null)
                advocatesByDiscord.put(advocate.getDsUserId(), advocate);
        }
        session.close();
    }

    private Client addClient(@Nullable Client client) {
        if (client != null) {
            clientsByPass.put(client.getPassport(), client);
            clientsByDiscord.put(client.getDsUserId(), client);
        }
        return client;
    }

    private <T> List<T> loadAllData(Class<T> type, Session session) {
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<T> criteria = builder.createQuery(type);
        criteria.from(type);
        return session.createQuery(criteria).getResultList();
    }

    public Client getClientByPass(@Nullable Integer passport) {
        if (passport == null) return null;
        return clientsByPass.get(passport);
    }

    public Client getClientByDiscord(long dsId) {
        return clientsByDiscord.get(dsId);
    }

    @Nullable public Client getClient(long dsId, Integer pass) {
        Client client = clientsByDiscord.get(dsId);
        if (client == null) {
            client = getClientByPass(pass);
        }
        return client;
    }

    public Client getOrCreateClient(Integer passport, long dsId, String name) {
        Client client = getClient(dsId, passport);
        if (client == null)
           return saveClient(new Client(passport, dsId, name, -1, false));
        return client;
    }

    public Advocate getAdvocateByDiscord(long dsId) {
        return advocatesByDiscord.get(dsId);
    }

    public Client saveClient(@NotNull Client client) {
        sessionFactory.inTransaction(session -> session.merge(client));
        clientsByPass.put(client.getPassport(), client);
        clientsByDiscord.put(client.getDsUserId(), client);
        return client;
    }

    public void saveAdvocate(@NotNull Advocate advocate) {
        sessionFactory.inTransaction(session -> session.merge(advocate));
        advocatesByDiscord.put(advocate.getDsUserId(), advocate);
    }

    public Client updateClient(Integer passport, long dsId, String name, long dsChannelId, boolean signed) {
        Client old = clientsByDiscord.get(dsId);

        if (old == null) {
            old = getClientByPass(passport);
            if (old == null) {
                return saveClient(addClient(new Client(passport, dsId, name, dsChannelId, signed)));
            } else {
                log.warn("Suspicious discord id change of client w/ pass {}: {} -> {}.",
                        passport, old.getDsUserId(), dsId);
                clientsByDiscord.remove(old.getDsUserId());
            }
        }

        if (name != null)
            old.setName(name);
        old.setDsUserId(dsId);
        old.setSigned(signed);
        if (passport != null) {
            if (passport != old.getPassport())
                clientsByPass.remove(old.getPassport());
            old.setPassport(passport);
        }
        old.setDsUserChannel(dsChannelId);

        return saveClient(old);
    }

    public Map<Integer, Client> getClientsByPass() {
        return clientsByPass;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void close() {
        sessionFactory.close();
    }

}
