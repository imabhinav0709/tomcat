import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StudentServlet extends HttpServlet {

    private static final String JDBC_URL = "jdbc:h2:file:/workspaces/tomcat/apache-tomcat-10.1.42/temp/testappdb;AUTO_SERVER=TRUE";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASSWORD = "";

    private volatile boolean initialized = false;

    private static final class StudentRecord {
        private final int id;
        private final String name;
        private final String email;

        private StudentRecord(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            HttpSession session = request.getSession(false);
            String loggedInUser = session == null ? "unknown" : (String) session.getAttribute("user");
            String message = request.getParameter("message");
            String error = request.getParameter("error");
            String editIdParam = request.getParameter("editId");

            ensureDatabaseInitialized();

            StudentRecord editingRecord = null;
            List<StudentRecord> students = new ArrayList<>();

            try (Connection conn = getConnection()) {
                if (editIdParam != null && !editIdParam.isBlank()) {
                    int editId = Integer.parseInt(editIdParam);
                    editingRecord = findStudentById(conn, editId);
                }

                students = listStudents(conn);
            }

            out.println("<html>");
            out.println("<head><title>Student Records</title></head>");
            out.println("<body>");
            out.println("<h1>Students CRUD (H2 Database)</h1>");
            out.println("<p>Logged in as: <strong>" + escapeHtml(loggedInUser) + "</strong></p>");

            if (message != null && !message.isBlank()) {
                out.println("<p style='color: green; font-weight: bold;'>" + escapeHtml(message) + "</p>");
            }

            if (error != null && !error.isBlank()) {
                out.println("<p style='color: red; font-weight: bold;'>" + escapeHtml(error) + "</p>");
            }

            out.println("<h2>Create Student</h2>");
            out.println("<form method='post' action='students'>");
            out.println("<input type='hidden' name='action' value='create'>");
            out.println("<p><label>Name: <input type='text' name='name' required maxlength='100'></label></p>");
            out.println("<p><label>Email: <input type='email' name='email' required maxlength='120'></label></p>");
            out.println("<p><button type='submit'>Create</button></p>");
            out.println("</form>");

            out.println("<h2>Update Student</h2>");
            if (editingRecord != null) {
                out.println("<form method='post' action='students'>");
                out.println("<input type='hidden' name='action' value='update'>");
                out.println("<input type='hidden' name='id' value='" + editingRecord.id + "'>");
                out.println("<p><label>Name: <input type='text' name='name' required maxlength='100' value='" + escapeHtml(editingRecord.name) + "'></label></p>");
                out.println("<p><label>Email: <input type='email' name='email' required maxlength='120' value='" + escapeHtml(editingRecord.email) + "'></label></p>");
                out.println("<p><button type='submit'>Update</button> <a href='students'>Cancel</a></p>");
                out.println("</form>");
            } else {
                out.println("<p>Select a student from the table below to edit.</p>");
            }

            out.println("<h2>All Students</h2>");
            out.println("<table border='1' cellpadding='6' cellspacing='0'>");
            out.println("<tr><th>ID</th><th>Name</th><th>Email</th><th>Actions</th></tr>");
            for (StudentRecord student : students) {
                out.println("<tr>");
                out.println("<td>" + student.id + "</td>");
                out.println("<td>" + escapeHtml(student.name) + "</td>");
                out.println("<td>" + escapeHtml(student.email) + "</td>");
                out.println("<td>");
                out.println("<a href='students?editId=" + student.id + "'>Edit</a> ");
                out.println("<form method='post' action='students' style='display:inline;'>");
                out.println("<input type='hidden' name='action' value='delete'>");
                out.println("<input type='hidden' name='id' value='" + student.id + "'>");
                out.println("<button type='submit' onclick=\"return confirm('Delete this student?');\">Delete</button>");
                out.println("</form>");
                out.println("</td>");
                out.println("</tr>");
            }
            out.println("</table>");

            out.println("<p><a href='secure.jsp'>Open Secure JSP Page</a></p>");
            out.println("<p><a href='logout'>Logout</a></p>");
            out.println("<p><a href='index.html'>Back to Home</a></p>");
            out.println("</body>");
            out.println("</html>");

        } catch (NumberFormatException nfe) {
            throw new ServletException("Invalid student id value.", nfe);
        } catch (ClassNotFoundException cnfEx) {
            throw new ServletException("H2 Driver not found. Ensure h2.jar is in Tomcat lib.", cnfEx);
        } catch (SQLException sqlEx) {
            throw new ServletException("Failed to access students database.", sqlEx);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null || action.isBlank()) {
            redirectWithMessage(response, request, "error", "Action is required.");
            return;
        }

        try {
            ensureDatabaseInitialized();
            switch (action) {
                case "create":
                    createStudent(request);
                    redirectWithMessage(response, request, "message", "Student created successfully.");
                    return;
                case "update":
                    updateStudent(request);
                    redirectWithMessage(response, request, "message", "Student updated successfully.");
                    return;
                case "delete":
                    deleteStudent(request);
                    redirectWithMessage(response, request, "message", "Student deleted successfully.");
                    return;
                default:
                    redirectWithMessage(response, request, "error", "Unknown action: " + action);
                    return;
            }
        } catch (ClassNotFoundException cnfEx) {
            throw new ServletException("H2 Driver not found. Ensure h2.jar is in Tomcat lib.", cnfEx);
        } catch (SQLException | IllegalArgumentException ex) {
            redirectWithMessage(response, request, "error", ex.getMessage());
        }
    }

    private void ensureDatabaseInitialized() throws SQLException, ClassNotFoundException {
        if (initialized) {
            return;
        }

        synchronized (this) {
            if (initialized) {
                return;
            }

            Class.forName("org.h2.Driver");
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS students (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, email VARCHAR(120) NOT NULL UNIQUE)");
            }

            seedDataIfEmpty();
            initialized = true;
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    private void seedDataIfEmpty() throws SQLException {
        try (Connection conn = getConnection();
             Statement countStmt = conn.createStatement();
             ResultSet rs = countStmt.executeQuery("SELECT COUNT(*) FROM students")) {
            rs.next();
            if (rs.getInt(1) > 0) {
                return;
            }
        }

        try (Connection conn = getConnection();
             PreparedStatement insert = conn.prepareStatement("INSERT INTO students (name, email) VALUES (?, ?)")) {
            insert.setString(1, "Alice");
            insert.setString(2, "alice@example.com");
            insert.executeUpdate();

            insert.setString(1, "Bob");
            insert.setString(2, "bob@example.com");
            insert.executeUpdate();

            insert.setString(1, "Charlie");
            insert.setString(2, "charlie@example.com");
            insert.executeUpdate();
        }
    }

    private List<StudentRecord> listStudents(Connection conn) throws SQLException {
        List<StudentRecord> students = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, email FROM students ORDER BY id")) {
            while (rs.next()) {
                students.add(new StudentRecord(rs.getInt("id"), rs.getString("name"), rs.getString("email")));
            }
        }
        return students;
    }

    private StudentRecord findStudentById(Connection conn, int id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT id, name, email FROM students WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new StudentRecord(rs.getInt("id"), rs.getString("name"), rs.getString("email"));
                }
            }
        }
        return null;
    }

    private void createStudent(HttpServletRequest request) throws SQLException {
        String name = requiredField(request.getParameter("name"), "Name is required.");
        String email = requiredField(request.getParameter("email"), "Email is required.");

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO students (name, email) VALUES (?, ?)")) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.executeUpdate();
        }
    }

    private void updateStudent(HttpServletRequest request) throws SQLException {
        int id = parseId(request.getParameter("id"));
        String name = requiredField(request.getParameter("name"), "Name is required.");
        String email = requiredField(request.getParameter("email"), "Email is required.");

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE students SET name = ?, email = ? WHERE id = ?")) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setInt(3, id);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("Student not found for update.");
            }
        }
    }

    private void deleteStudent(HttpServletRequest request) throws SQLException {
        int id = parseId(request.getParameter("id"));

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM students WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private int parseId(String idValue) {
        if (idValue == null || idValue.isBlank()) {
            throw new IllegalArgumentException("Student id is required.");
        }

        try {
            return Integer.parseInt(idValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Student id must be a number.");
        }
    }

    private String requiredField(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    private void redirectWithMessage(HttpServletResponse response, HttpServletRequest request, String key, String message)
            throws IOException {
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/students?" + key + "=" + encoded);
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
