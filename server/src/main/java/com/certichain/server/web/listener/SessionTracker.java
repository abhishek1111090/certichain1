package com.certichain.server.web.listener;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import java.util.concurrent.atomic.AtomicInteger;

/** Servlet container listener counting live HTTP sessions (shown on the dashboard). */
@WebListener
public class SessionTracker implements HttpSessionListener {

    private static final AtomicInteger ACTIVE = new AtomicInteger();

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        ACTIVE.incrementAndGet();
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        ACTIVE.updateAndGet(n -> Math.max(0, n - 1));
    }

    public static int activeSessions() {
        return ACTIVE.get();
    }
}
