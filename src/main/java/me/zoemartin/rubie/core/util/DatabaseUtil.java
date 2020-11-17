package me.zoemartin.rubie.core.util;

import org.hibernate.*;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import javax.persistence.criteria.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class DatabaseUtil {
    private static final Collection<Class<?>> mapped = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static StandardServiceRegistry registry;
    private static SessionFactory sessionFactory;
    private static Configuration config;

    public static void setConfig(Configuration configuration) {
        config = configuration;
        mapped.forEach(configuration::addAnnotatedClass);
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                registry = new StandardServiceRegistryBuilder().applySettings(config.getProperties()).build();
                sessionFactory = config.buildSessionFactory(registry);
            } catch (Exception e) {
                e.printStackTrace();
                if (registry != null) {
                    StandardServiceRegistryBuilder.destroy(registry);
                }
            }
        }
        return sessionFactory;
    }

    public static void saveObject(Object... objects) {
        Transaction transaction = null;
        try (Session session = getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            for (Object object : objects) {
                session.save(object);
            }
            transaction.commit();
            session.close();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    public static void deleteObject(Object... objects) {
        Transaction transaction = null;
        try (Session session = getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            for (Object object : objects) {
                session.delete(object);
            }
            transaction.commit();
            session.close();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    public static void updateObject(Object... objects) {
        Transaction transaction = null;
        try (Session session = getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            for (Object object : objects) {
                session.update(object);
            }
            transaction.commit();
            session.close();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    public static <T> List<T> find(Class<T> clazz, Predicate... predicates) {
        Session s = getSessionFactory().openSession();
        CriteriaBuilder cb = s.getCriteriaBuilder();

        CriteriaQuery<T> q = cb.createQuery(clazz);
        Root<T> r = q.from(clazz);
        return s.createQuery(q.select(r).where(predicates)).getResultList();
    }

    public static void shutdown() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    public static void setMapped(Class<?> aClass){
        mapped.add(aClass);
    }

    private static <T, R> Collection<R> loadCollection(String queryString, Class<T> tClass, Function<? super T, ? extends R> mapper) {
        Session session = getSessionFactory().openSession();
        Collection<T> load = session.createQuery(queryString, tClass).list();
        return load.stream().map(mapper).collect(Collectors.toList());
    }

    public static <T, K, V> Map<K, V> loadMap(String queryString, Class<T> tClass,
                                              Function<? super T, ? extends K> keyMapper,
                                              Function<? super T, ? extends V> valueMapper) {
        Session session = getSessionFactory().openSession();
        Collection<T> load = session.createQuery(queryString, tClass).list();
        return load.stream().collect(Collectors.toConcurrentMap(keyMapper, valueMapper));
    }

    public static <T, R, K, V> Map<R, ? extends Map<K, V>> loadGroupedMap(String queryString, Class<T> tClass,
                                                                          Function<? super T, ? extends R> groupingBy,
                                                                          Function<? super T, ? extends K> keyMapper,
                                                                          Function<? super T, ? extends V> valueMapper) {

        Session session = getSessionFactory().openSession();
        Collection<T> load = session.createQuery(queryString, tClass).list();

        return load.stream().collect(Collectors.groupingByConcurrent(groupingBy,
            Collectors.mapping(Function.identity(),
                Collectors.toConcurrentMap(keyMapper, valueMapper))));
    }

    public static <T, R, K> Map<R, ? extends Collection<K>> loadGroupedCollection(String queryString, Class<T> tClass,
                                                                                  Function<? super T, ? extends R> groupingBy,
                                                                                  Function<? super T, ? extends K> mapper,
                                                                                  Collector<K, ?, ? extends Collection<K>> collector) {

        Session session = getSessionFactory().openSession();
        Collection<T> load = session.createQuery(queryString, tClass).list();

        return load.stream().collect(Collectors.groupingByConcurrent(groupingBy,
            Collectors.mapping(mapper, collector)));
    }
}
