package com.certichain.desktop.ui;

import com.certichain.core.exception.InvalidCertificateException;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Shared colours, fonts and helpers so every screen looks the same. */
public final class Ui {

    public static final Color BRAND = new Color(0x1D3F8F);
    public static final Color SIDE = new Color(0x0C1733);
    public static final Color SIDE_INK = new Color(0xC7D2FE);
    public static final Color BG = new Color(0xF4F6FB);
    public static final Color LINE = new Color(0xE2E8F0);
    public static final Color MUTED = new Color(0x64748B);
    public static final Color OK = new Color(0x047857);
    public static final Color OK_SOFT = new Color(0xE7F7EF);
    public static final Color WARN = new Color(0xB45309);
    public static final Color WARN_SOFT = new Color(0xFFF4E0);
    public static final Color BAD = new Color(0xC62828);
    public static final Color BAD_SOFT = new Color(0xFDECEC);

    private Ui() {
    }

    public static Font font(int style, float size) {
        return new JLabel().getFont().deriveFont(style, size);
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.BOLD, 20f));
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        return l;
    }

    public static Border pad(int all) {
        return BorderFactory.createEmptyBorder(all, all, all, all);
    }

    /** White rounded "card" with a subtle border. */
    public static JPanel card(JComponent content) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.putClientProperty("FlatLaf.style", "arc: 14");
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE, 1, true), pad(16)));
        p.add(content);
        return p;
    }

    /** Header area used at the top of every screen. */
    public static JPanel header(String title, String subtitle) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        p.add(title(title), BorderLayout.NORTH);
        p.add(muted(subtitle), BorderLayout.CENTER);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        return p;
    }

    /**
     * Runs {@code work} on a SwingWorker thread so the window never freezes during database
     * work, then hands the result to {@code done} back on the Event Dispatch Thread.
     */
    public static <T> void background(Component parent, Callable<T> work, Consumer<T> done) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                try {
                    done.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    error(parent, e.getCause());
                }
            }
        }.execute();
    }

    public static void error(Component parent, Throwable t) {
        String msg = t instanceof InvalidCertificateException ice
                ? "<html>Please fix:<ul><li>" + String.join("<li>", ice.getErrors()) + "</ul></html>"
                : t.getMessage();
        JOptionPane.showMessageDialog(parent, msg, "CertiChain", JOptionPane.ERROR_MESSAGE);
    }

    public static String html(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
