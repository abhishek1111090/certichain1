package com.certichain.desktop.ui;

import com.certichain.desktop.dao.AccountDao;
import com.certichain.desktop.db.Database;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Optional;

/** Modal sign-in window. Checks the password against the same PBKDF2 hashes as the web console. */
public class LoginDialog extends JDialog {

    private final Database db;
    private final JTextField username = new JTextField("admin", 18);
    private final JPasswordField password = new JPasswordField(18);
    private final JLabel message = new JLabel(" ");
    private AccountDao.Account account;
    private int failures;

    public LoginDialog(Database db) {
        super((java.awt.Frame) null, "CertiChain Registrar Desk", true);
        this.db = db;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel art = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, Ui.SIDE, getWidth(), getHeight(), new Color(0x0F9D94)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        art.setPreferredSize(new Dimension(260, 340));
        art.setBorder(Ui.pad(26));
        JLabel brand = new JLabel("<html><div style='font-size:20px;font-weight:bold;color:white'>CertiChain</div>"
                + "<div style='color:#c7d2fe'>Registrar Desk</div></html>");
        JLabel tagline = new JLabel("<html><div style='color:white;font-size:13px'>Bulk issue, audit and "
                + "tamper-test the credential ledger.</div><br><div style='color:#c7d2fe;font-size:10px'>Swing &middot; JDBC &middot; H2</div></html>");
        art.add(brand, BorderLayout.NORTH);
        art.add(tagline, BorderLayout.SOUTH);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        JLabel title = new JLabel("Sign in");
        title.setFont(Ui.font(Font.BOLD, 22f));
        form.add(left(title));
        form.add(left(Ui.muted("Use your web console account")));
        form.add(Box.createVerticalStrut(18));
        form.add(left(new JLabel("Username")));
        form.add(left(username));
        form.add(Box.createVerticalStrut(10));
        form.add(left(new JLabel("Password")));
        form.add(left(password));
        form.add(Box.createVerticalStrut(8));
        message.setForeground(Ui.BAD);
        form.add(left(message));
        form.add(Box.createVerticalStrut(10));
        JButton signIn = new JButton("Sign in");
        signIn.putClientProperty("JButton.buttonType", "default");
        signIn.setBackground(Ui.BRAND);
        signIn.setForeground(Color.WHITE);
        signIn.addActionListener(e -> attempt());
        form.add(left(signIn));
        form.add(Box.createVerticalGlue());
        form.add(left(Ui.muted("<html><small>" + Ui.html(shorten(db.getUrl())) + "</small></html>")));
        password.addActionListener(e -> attempt());
        getRootPane().setDefaultButton(signIn);

        getContentPane().add(art, BorderLayout.WEST);
        getContentPane().add(form, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    public Optional<AccountDao.Account> showDialog() {
        setVisible(true);
        return Optional.ofNullable(account);
    }

    private void attempt() {
        if (failures >= 5) {
            message.setText("Too many attempts. Restart the application.");
            return;
        }
        char[] pw = password.getPassword();
        try (Connection c = db.connect()) {
            Optional<AccountDao.Account> a = new AccountDao().authenticate(c, username.getText(), pw);
            if (a.isPresent()) {
                account = a.get();
                dispose();
            } else {
                failures++;
                message.setText("Invalid username or password.");
                password.setText("");
            }
        } catch (Exception ex) {
            message.setText("Database error: " + ex.getMessage());
        } finally {
            Arrays.fill(pw, '\0');
        }
    }

    private static String shorten(String url) {
        return url.length() > 48 ? "..." + url.substring(url.length() - 48) : url;
    }

    private static Component left(javax.swing.JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (c instanceof JTextField || c instanceof JButton) {
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        }
        return c;
    }
}
