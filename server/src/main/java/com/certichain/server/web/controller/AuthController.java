package com.certichain.server.web.controller;

import com.certichain.server.config.InstitutionProfile;
import com.certichain.server.model.AdminSession;
import com.certichain.server.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;

@Controller
public class AuthController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthService authService;
    private final InstitutionProfile institution;
    private final boolean demoMode;

    public AuthController(AuthService authService, InstitutionProfile institution,
                          @Value("${certichain.seed-demo:true}") boolean demoMode) {
        this.authService = authService;
        this.institution = institution;
        this.demoMode = demoMode;
    }

    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String next, HttpSession session, Model model) {
        if (session.getAttribute(AdminSession.ATTR) != null) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("next", safeNext(next));
        model.addAttribute("institution", institution);
        model.addAttribute("demoMode", demoMode);
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        @RequestParam(required = false) String next, HttpServletRequest request, Model model) {
        model.addAttribute("institution", institution);
        model.addAttribute("demoMode", demoMode);
        model.addAttribute("next", safeNext(next));
        model.addAttribute("username", username);
        Optional<AdminSession> admin;
        try {
            admin = authService.login(username, password, request.getRemoteAddr());
        } catch (AuthService.LockedOutException e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
        if (admin.isEmpty()) {
            model.addAttribute("error", "Invalid username or password. Accounts are created by the administrator; there is no self sign-up.");
            return "login";
        }
        // Session fixation defence: throw away any pre-login session and start a fresh one.
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(AdminSession.ATTR, admin.get());
        byte[] token = new byte[24];
        RANDOM.nextBytes(token);
        session.setAttribute(AdminSession.CSRF_ATTR, HexFormat.of().formatHex(token));
        return "redirect:" + safeNext(next);
    }

    @PostMapping("/admin/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?bye";
    }

    /** Only allow redirects back into the admin area (no open redirect to other sites). */
    private static String safeNext(String next) {
        return next != null && next.matches("/admin(/[A-Za-z0-9/_-]*)?") ? next : "/admin/dashboard";
    }
}
