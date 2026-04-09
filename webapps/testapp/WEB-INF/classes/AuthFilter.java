import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // No startup configuration required for this demo filter.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // A user is considered authenticated when session contains "user" attribute.
        HttpSession session = httpRequest.getSession(false);
        boolean authenticated = session != null && session.getAttribute("user") != null;

        if (authenticated) {
            chain.doFilter(request, response);
            return;
        }

        // Redirect unauthenticated access attempts to login page.
        String loginPath = httpRequest.getContextPath() + "/login.jsp";
        httpResponse.sendRedirect(loginPath);
    }

    @Override
    public void destroy() {
        // No cleanup work required for this demo filter.
    }
}
