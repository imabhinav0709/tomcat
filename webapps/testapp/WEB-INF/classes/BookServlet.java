import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.List;

public class BookServlet extends HttpServlet {

    private final BookDao bookDao = new BookDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (action == null || action.isBlank() || "list".equalsIgnoreCase(action)) {
            renderList(request, response, null);
            return;
        }

        if ("new".equalsIgnoreCase(action)) {
            renderForm(request, response, new Book(), "create", "Create Book");
            return;
        }

        if ("edit".equalsIgnoreCase(action)) {
            String idParam = request.getParameter("id");
            if (idParam == null || idParam.isBlank()) {
                response.sendRedirect(request.getContextPath() + "/books");
                return;
            }

            try {
                Long id = Long.valueOf(idParam);
                Optional<Book> bookOptional = bookDao.findById(id);
                if (bookOptional.isEmpty()) {
                    renderList(request, response, "Book not found.");
                    return;
                }

                renderForm(request, response, bookOptional.get(), "update", "Edit Book");
                return;
            } catch (NumberFormatException ex) {
                renderList(request, response, "Invalid book id.");
                return;
            }
        }

        if ("delete".equalsIgnoreCase(action)) {
            String idParam = request.getParameter("id");
            if (idParam != null && !idParam.isBlank()) {
                try {
                    bookDao.delete(Long.valueOf(idParam));
                } catch (NumberFormatException ex) {
                    renderList(request, response, "Invalid book id.");
                    return;
                }
            }

            response.sendRedirect(request.getContextPath() + "/books?message=deleted");
            return;
        }

        renderList(request, response, "Unknown action.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        try {
            if ("create".equalsIgnoreCase(action)) {
                Book book = buildBookFromRequest(request, null);
                bookDao.create(book);
                response.sendRedirect(request.getContextPath() + "/books?message=created");
                return;
            }

            if ("update".equalsIgnoreCase(action)) {
                Long id = Long.valueOf(request.getParameter("id"));
                Book book = buildBookFromRequest(request, id);
                bookDao.update(book);
                response.sendRedirect(request.getContextPath() + "/books?message=updated");
                return;
            }

            if ("delete".equalsIgnoreCase(action)) {
                Long id = Long.valueOf(request.getParameter("id"));
                bookDao.delete(id);
                response.sendRedirect(request.getContextPath() + "/books?message=deleted");
                return;
            }

            response.sendRedirect(request.getContextPath() + "/books");
        } catch (Exception ex) {
            throw new ServletException("Error processing book CRUD request", ex);
        }
    }
    }

    private void renderList(HttpServletRequest request, HttpServletResponse response, String notice)
            throws IOException {

        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            HttpSession session = request.getSession(false);
            String loggedInUser = session == null ? "unknown" : (String) session.getAttribute("user");

            // Ensure demo data exists before reading from the books table.
            bookDao.seedSampleBooksIfEmpty();
            List<Book> books = bookDao.findAll();

            out.println("<html>");
            out.println("<head><title>Books (Hibernate CRUD)</title></head>");
            out.println("<body>");
            out.println("<h1>Books from Hibernate Entity Mapping</h1>");
            out.println("<p>Logged in as: <strong>" + escapeHtml(loggedInUser) + "</strong></p>");

            String message = request.getParameter("message");
            if (notice != null) {
                out.println("<p style='color:red;'>" + escapeHtml(notice) + "</p>");
            } else if ("created".equalsIgnoreCase(message)) {
                out.println("<p style='color:green;'>Book created successfully.</p>");
            } else if ("updated".equalsIgnoreCase(message)) {
                out.println("<p style='color:green;'>Book updated successfully.</p>");
            } else if ("deleted".equalsIgnoreCase(message)) {
                out.println("<p style='color:green;'>Book deleted successfully.</p>");
            }

            out.println("<p><a href='books?action=new'>Add New Book</a></p>");
            out.println("<table border='1' cellpadding='6' cellspacing='0'>");
            out.println("<tr><th>ID</th><th>Title</th><th>Author</th><th>ISBN</th><th>Published Year</th><th>Actions</th></tr>");

            for (Book book : books) {
                out.println("<tr>");
                out.println("<td>" + book.getId() + "</td>");
                out.println("<td>" + escapeHtml(book.getTitle()) + "</td>");
                out.println("<td>" + escapeHtml(book.getAuthor()) + "</td>");
                out.println("<td>" + escapeHtml(book.getIsbn()) + "</td>");
                out.println("<td>" + book.getPublishedYear() + "</td>");
                out.println("<td>");
                out.println("<a href='books?action=edit&id=" + book.getId() + "'>Edit</a> | ");
                out.println("<form action='books' method='post' style='display:inline;' onsubmit=\"return confirm('Delete this book?');\">");
                out.println("<input type='hidden' name='action' value='delete'>");
                out.println("<input type='hidden' name='id' value='" + book.getId() + "'>");
                out.println("<button type='submit'>Delete</button>");
                out.println("</form>");
                out.println("</td>");
                out.println("</tr>");
            }

            out.println("</table>");
            out.println("<p><a href='students'>View Student Servlet</a></p>");
            out.println("<p><a href='secure.jsp'>Back to Secure JSP</a></p>");
            out.println("<p><a href='logout'>Logout</a></p>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    private void renderForm(HttpServletRequest request, HttpServletResponse response, Book book, String action, String title)
            throws IOException {

        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            HttpSession session = request.getSession(false);
            String loggedInUser = session == null ? "unknown" : (String) session.getAttribute("user");

            out.println("<html>");
            out.println("<head><title>" + escapeHtml(title) + "</title></head>");
            out.println("<body>");
            out.println("<h1>" + escapeHtml(title) + "</h1>");
            out.println("<p>Logged in as: <strong>" + escapeHtml(loggedInUser) + "</strong></p>");

            out.println("<form action='books' method='post'>");
            out.println("<input type='hidden' name='action' value='" + escapeHtml(action) + "'>");
            if (book.getId() != null) {
                out.println("<input type='hidden' name='id' value='" + book.getId() + "'>");
            }
            out.println("<p><label>Title: <input type='text' name='title' value='" + escapeHtml(book.getTitle()) + "' required></label></p>");
            out.println("<p><label>Author: <input type='text' name='author' value='" + escapeHtml(book.getAuthor()) + "' required></label></p>");
            out.println("<p><label>ISBN: <input type='text' name='isbn' value='" + escapeHtml(book.getIsbn()) + "' required></label></p>");
            out.println("<p><label>Published Year: <input type='number' name='publishedYear' value='" + (book.getPublishedYear() == null ? "" : book.getPublishedYear()) + "' required></label></p>");
            out.println("<p><button type='submit'>Save</button></p>");
            out.println("</form>");

            out.println("<p><a href='books'>Back to Books List</a></p>");
            out.println("<p><a href='logout'>Logout</a></p>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    private Book buildBookFromRequest(HttpServletRequest request, Long id) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(request.getParameter("title"));
        book.setAuthor(request.getParameter("author"));
        book.setIsbn(request.getParameter("isbn"));
        String year = request.getParameter("publishedYear");
        book.setPublishedYear(year == null || year.isBlank() ? null : Integer.valueOf(year));
        return book;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
