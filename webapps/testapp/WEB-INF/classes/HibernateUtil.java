import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class HibernateUtil {

    private static final SessionFactory SESSION_FACTORY = buildSessionFactory();

    private HibernateUtil() {
    }

    private static SessionFactory buildSessionFactory() {
        try {
            // Build SessionFactory from hibernate.cfg.xml on classpath.
            return new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError("Failed to create SessionFactory: " + ex.getMessage());
        }
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}
