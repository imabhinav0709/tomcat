import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Destroy active session so user is logged out.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Return to login page after logout.
        response.sendRedirect(request.getContextPath() + "/login.jsp?logout=1");
    }
}
