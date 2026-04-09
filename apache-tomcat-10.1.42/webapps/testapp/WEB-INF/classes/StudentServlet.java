import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class StudentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Set response type so browser renders HTML content.
        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            // Read authenticated user from session for display.
            HttpSession session = request.getSession(false);
            String loggedInUser = session == null ? "unknown" : (String) session.getAttribute("user");

            out.println("<html>");
            out.println("<head><title>Student Records</title></head>");
            out.println("<body>");
            out.println("<h1>Students from H2 In-Memory Database</h1>");
            out.println("<p>Logged in as: <strong>" + loggedInUser + "</strong></p>");

            // Load H2 JDBC driver.
            Class.forName("org.h2.Driver");

            // Open connection to in-memory H2 database.
            // DB_CLOSE_DELAY=-1 keeps DB alive while JVM is running.
            try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
                 Statement stmt = conn.createStatement()) {

                // Create table if it does not already exist.
                stmt.execute("CREATE TABLE IF NOT EXISTS students (id INT, name VARCHAR(100))");

                // Reset sample data on each request for a predictable demo output.
                stmt.execute("DELETE FROM students");
                stmt.execute("INSERT INTO students (id, name) VALUES (1, 'Alice')");
                stmt.execute("INSERT INTO students (id, name) VALUES (2, 'Bob')");
                stmt.execute("INSERT INTO students (id, name) VALUES (3, 'Charlie')");

                // Query all student records.
                try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM students")) {
                    // Render records as an HTML table.
                    out.println("<table border='1' cellpadding='6' cellspacing='0'>");
                    out.println("<tr><th>ID</th><th>Name</th></tr>");

                    while (rs.next()) {
                        out.println("<tr>");
                        out.println("<td>" + rs.getInt("id") + "</td>");
                        out.println("<td>" + rs.getString("name") + "</td>");
                        out.println("</tr>");
                    }

                    out.println("</table>");
                }
            } catch (Exception dbEx) {
                // Show JDBC/database related errors on the page.
                out.println("<p><strong>Database Error:</strong> " + dbEx.getMessage() + "</p>");
            }

            out.println("<p><a href='secure.jsp'>Open Secure JSP Page</a></p>");
            out.println("<p><a href='logout'>Logout</a></p>");
            out.println("<p><a href='index.html'>Back to Home</a></p>");
            out.println("</body>");
            out.println("</html>");

        } catch (ClassNotFoundException cnfEx) {
            throw new ServletException("H2 Driver not found. Ensure h2.jar is in Tomcat lib.", cnfEx);
        }
    }
}
