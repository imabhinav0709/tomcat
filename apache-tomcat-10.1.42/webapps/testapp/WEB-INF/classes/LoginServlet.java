import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Read credentials submitted from login.jsp form.
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Simple demo authentication: username=admin and password=admin123.
        if ("admin".equals(username) && "admin123".equals(password)) {
            HttpSession session = request.getSession(true);
            session.setAttribute("user", username);

            // Redirect to protected servlet after successful login.
            response.sendRedirect(request.getContextPath() + "/students");
            return;
        }

        // On invalid login, redirect back to login page with an error flag.
        response.sendRedirect(request.getContextPath() + "/login.jsp?error=1");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // If /login is requested directly, send user to login JSP.
        response.sendRedirect(request.getContextPath() + "/login.jsp");
    }
}
