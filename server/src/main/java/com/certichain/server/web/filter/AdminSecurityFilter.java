package com.certichain.server.web.filter;

import com.certichain.server.model.AdminSession;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Servlet filter guarding everything under /admin:
 * <ol>
 *   <li>Session management - no {@link AdminSession} in the HttpSession means redirect to /login.</li>
 *   <li>CSRF protection - every POST must echo the per-session token from a hidden form field.</li>
 *   <li>No caching of admin pages, so "Back" after logout does not show data.</li>
 * </ol>
 */
@WebFilter(urlPatterns = "/admin/*")
public class AdminSecurityFilter extends HttpFilter {

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpSession session = req.getSession(false);
        AdminSession admin = session == null ? null : (AdminSession) session.getAttribute(AdminSession.ATTR);
        if (admin == null) {
            String target = req.getRequestURI().substring(req.getContextPath().length());
            res.sendRedirect(req.getContextPath() + "/login?next=" + URLEncoder.encode(target, StandardCharsets.UTF_8));
            return;
        }
        if ("POST".equalsIgnoreCase(req.getMethod())) {
            String expected = (String) session.getAttribute(AdminSession.CSRF_ATTR);
            String actual = req.getParameter("_csrf");
            if (expected == null || actual == null
                    || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token");
                return;
            }
        }
        res.setHeader("Cache-Control", "no-store");
        chain.doFilter(req, res);
    }
}
