package com.certichain.desktop;

import com.certichain.desktop.dao.AccountDao;
import com.certichain.desktop.db.Database;
import com.certichain.desktop.service.RegistrarService;
import com.certichain.desktop.ui.LoginDialog;
import com.certichain.desktop.ui.MainFrame;
import com.certichain.desktop.ui.Ui;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.sql.Connection;
import java.util.Optional;

/**
 * CertiChain Registrar Desk - the Swing desktop client used in the exam cell for bulk
 * issuing, integrity audits and the tamper demonstration. It talks to the database
 * directly over JDBC (no server needed), on the same ledger the web portal uses.
 */
public final class RegistrarDesk {

    private RegistrarDesk() {
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("Panel.background", Ui.BG);

        Database db = Database.load();
        SwingUtilities.invokeLater(() -> start(db));
    }

    private static void start(Database db) {
        try {
            db.initSchema();
            AccountDao accounts = new AccountDao();
            try (Connection c = db.connect()) {
                if (accounts.countUsers(c) == 0) {
                    accounts.createDefaultAccounts(c);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Cannot open the database:\n" + db.getUrl() + "\n\n" + e.getMessage(),
                    "CertiChain", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        Optional<AccountDao.Account> account = new LoginDialog(db).showDialog();
        if (account.isEmpty()) {
            System.exit(0);
        }
        new MainFrame(new RegistrarService(db), account.get()).setVisible(true);
    }
}
