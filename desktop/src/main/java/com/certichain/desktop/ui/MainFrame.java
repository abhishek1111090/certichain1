package com.certichain.desktop.ui;

import com.certichain.desktop.dao.AccountDao;
import com.certichain.desktop.service.RegistrarService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;

/** Main window: dark sidebar navigation on the left, screens in a CardLayout on the right. */
public class MainFrame extends JFrame {

    private final RegistrarService service;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JComponent> screens = new LinkedHashMap<>();
    private final JLabel status = new JLabel(" ");

    public MainFrame(RegistrarService service, AccountDao.Account account) {
        super("CertiChain Registrar Desk");
        this.service = service;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 700));

        screens.put("Dashboard", new DashboardPanel(service));
        screens.put("Certificates", new CertificatesPanel(service, account));
        screens.put("Issue certificate", new IssuePanel(service, account, this::refreshStatus));
        screens.put("Bulk import (CSV)", new BulkImportPanel(service, account, this::refreshStatus));
        screens.put("Integrity audit", new IntegrityPanel(service, account));
        if (account.isAdmin()) {
            screens.put("Tamper lab", new TamperLabPanel(service, account));
        }

        content.setBackground(Ui.BG);
        content.setBorder(Ui.pad(22));
        screens.forEach((name, panel) -> content.add(panel, name));

        getContentPane().add(sidebar(account), BorderLayout.WEST);
        getContentPane().add(content, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Ui.LINE), BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        status.setForeground(Ui.MUTED);
        status.setOpaque(true);
        status.setBackground(Color.WHITE);
        getContentPane().add(status, BorderLayout.SOUTH);

        show("Dashboard");
        refreshStatus();
        // Keeps the status bar current when the web portal issues certificates meanwhile.
        new Timer(15_000, e -> refreshStatus()).start();
        pack();
        setSize(1240, 780);
        setLocationRelativeTo(null);
    }

    private JPanel sidebar(AccountDao.Account account) {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Ui.SIDE);
        side.setBorder(BorderFactory.createEmptyBorder(20, 12, 16, 12));
        side.setPreferredSize(new Dimension(230, 0));

        JLabel brand = new JLabel("<html><span style='font-size:15px;font-weight:bold;color:white'>CertiChain</span><br>"
                + "<span style='color:#c7d2fe;font-size:10px'>Registrar Desk</span></html>");
        brand.setBorder(BorderFactory.createEmptyBorder(0, 8, 20, 0));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(brand);

        ButtonGroup group = new ButtonGroup();
        boolean first = true;
        for (String name : screens.keySet()) {
            JToggleButton b = new JToggleButton(name);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            b.setHorizontalAlignment(JToggleButton.LEFT);
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setFont(Ui.font(Font.PLAIN, 13.5f));
            b.putClientProperty("FlatLaf.style", "background: #0c1733; foreground: #c7d2fe; selectedBackground: #24345e;"
                    + " selectedForeground: #ffffff; hoverBackground: #17244a; borderWidth: 0; focusWidth: 0; arc: 10;"
                    + " margin: 8,12,8,12");
            b.addActionListener(e -> show(name));
            b.setSelected(first);
            first = false;
            group.add(b);
            side.add(b);
            side.add(Box.createVerticalStrut(4));
        }
        side.add(Box.createVerticalGlue());
        JLabel who = new JLabel("<html><span style='color:white'>" + Ui.html(account.fullName()) + "</span><br>"
                + "<span style='color:#c7d2fe;font-size:10px'>" + account.role() + " &middot; @" + Ui.html(account.username())
                + "</span></html>");
        who.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        who.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(who);
        return side;
    }

    private void show(String name) {
        cards.show(content, name);
        if (screens.get(name) instanceof Refreshable r) {
            r.refresh();
        }
    }

    public void refreshStatus() {
        Ui.background(this, service::head, head -> status.setText("Connected to " + service.getDatabase().getUrl()
                + "     |     Ledger head #" + head.seq() + "  " + head.hash().substring(0, 16) + "..."));
    }
}
