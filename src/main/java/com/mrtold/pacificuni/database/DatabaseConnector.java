package com.mrtold.pacificuni.database;

import com.mrtold.pacificuni.model.Student;
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
    final Map<Integer, Student> studentsByPass = new HashMap<>();
    final Map<Long, Student> studentsByDiscord = new HashMap<>();

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
                            .addAnnotatedClass(Student.class)
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
        List<Student> students = loadAllData(Student.class, session);
        for (Student student : students) {
            addStudent(student);
        }
        session.close();
    }

    private Student addStudent(@Nullable Student student) {
        if (student != null) {
            studentsByPass.put(student.getPassport(), student);
            studentsByDiscord.put(student.getDsUserId(), student);
        }
        return student;
    }

    private <T> List<T> loadAllData(Class<T> type, Session session) {
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<T> criteria = builder.createQuery(type);
        criteria.from(type);
        return session.createQuery(criteria).getResultList();
    }

    public Student getStudentByPass(@Nullable Integer passport) {
        if (passport == null) return null;
        return studentsByPass.get(passport);
    }

    public Student getStudentByDiscord(long dsId) {
        return studentsByDiscord.get(dsId);
    }

    public void enrollStudent(int passport, long dsId, @Nullable String name, int courseId) {
        Student student = getStudentByDiscord(dsId);
        if (student == null) {
            student = getStudentByPass(passport);

            if (student == null) {
                student = addStudent(new Student(passport, dsId, name));
            } else {
                if (!student.getCourses().isEmpty()) {
                    log.warn("Suspicious discord id change of student w/ pass {} with active courses: {} -> {}.",
                            passport, student.getDsUserId(), dsId);
                }
                student.setDsUserId(dsId);
            }
        } else if (student.getPassport() != passport) {
            if (!student.getCourses().isEmpty()) {
                log.warn("Suspicious passport change of student w/ discord id {} with active courses: {} -> {}.",
                        dsId, student.getPassport(), passport);
            }
            student.setPassport(passport);
        }

        student.setName(name);

        if (student.getCourses().contains(courseId))
            log.warn("Repeatedly enrolling student w/ pass {} to course {}.", passport, courseId);

        student.getCourses().add(courseId);
        saveStudent(student);
    }

    public @Nullable Student expelStudent(long dsId, int courseId) {
        Student student = getStudentByDiscord(dsId);
        if (student == null) {
            return null;
        }

        if (!student.getCourses().contains(courseId))
            log.warn("Expelling student w/ discord id {} from course {} which he doesn't have.", dsId, courseId);
        else
            student.getCourses().remove(courseId);
        saveStudent(student);
        return student;
    }

    public void saveStudent(@NotNull Student student) {
        sessionFactory.inTransaction(session -> session.merge(student));
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void close() {
        sessionFactory.close();
    }

}
