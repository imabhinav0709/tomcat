import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class BookDao {

    public void seedSampleBooksIfEmpty() {
        Transaction tx = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Long count = session.createQuery("select count(b.id) from Book b", Long.class).uniqueResult();
            if (count == null || count == 0L) {
                session.persist(new Book("Clean Code", "Robert C. Martin", "9780132350884", 2008));
                session.persist(new Book("Effective Java", "Joshua Bloch", "9780134685991", 2018));
                session.persist(new Book("Design Patterns", "Erich Gamma", "9780201633610", 1994));
            }

            tx.commit();
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            throw ex;
        }
    }

    public List<Book> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Book b order by b.id", Book.class).list();
        }
    }

    public Optional<Book> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Book.class, id));
        }
    }

    public void create(Book book) {
        Transaction tx = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(book);
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            throw ex;
        }
    }

    public void update(Book book) {
        Transaction tx = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(book);
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            throw ex;
        }
    }

    public void delete(Long id) {
        Transaction tx = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Book book = session.get(Book.class, id);
            if (book != null) {
                session.remove(book);
            }
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            throw ex;
        }
    }
}
