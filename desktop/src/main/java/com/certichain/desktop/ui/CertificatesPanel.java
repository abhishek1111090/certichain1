package com.certichain.desktop.ui;

import com.certichain.core.model.Certificate;
import com.certichain.desktop.dao.AccountDao;
import com.certichain.desktop.service.RegistrarService;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.regex.Pattern;

/** Searchable register of all certificates, with revoke and "open verification page". */
public class CertificatesPanel extends JPanel implements Refreshable {

    private final RegistrarService service;
    private final AccountDao.Account account;
    private final CertificateTableModel model = new CertificateTableModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<CertificateTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField search = new JTextField(28);

    public CertificatesPanel(RegistrarService service, AccountDao.Account account) {
        super(new BorderLayout(0, 12));
        this.service = service;
        this.account = account;
        setOpaque(false);

        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(7).setCellRenderer(new StatusRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(260);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openVerification();
                }
            }
        });

        search.putClientProperty("JTextField.placeholderText", "Filter by name, roll no, certificate no, title...");
        search.putClientProperty("JTextField.showClearButton", true);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        JButton open = new JButton("Open verification page");
        open.addActionListener(e -> openVerification());
        JButton copy = new JButton("Copy verify link");
        copy.addActionListener(e -> copyLink());
        JButton revoke = new JButton("Revoke...");
        revoke.setForeground(Ui.BAD);
        revoke.setEnabled(account.isAdmin());
        revoke.setToolTipText(account.isAdmin() ? null : "Only ADMIN users can revoke");
        revoke.addActionListener(e -> revokeSelected());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(search);
        bar.add(refresh);
        bar.add(open);
        bar.add(copy);
        bar.add(revoke);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(Ui.header("Certificates", "Every certificate in the database. Double-click a row to verify it in the browser."), BorderLayout.NORTH);
        north.add(bar, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(Ui.card(new JScrollPane(table)), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Ui.background(this, service::allCertificates, model::setRows);
    }

    private void applyFilter() {
        String text = search.getText().trim();
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
    }

    private Certificate selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a certificate first.");
            return null;
        }
        return model.get(table.convertRowIndexToModel(row));
    }

    private String link(Certificate c) {
        return service.getDatabase().getPortalUrl() + "/verify/" + c.getCertNo();
    }

    private void openVerification() {
        Certificate c = selected();
        if (c == null) {
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(link(c)));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not open a browser. The link is:\n" + link(c));
        }
    }

    private void copyLink() {
        Certificate c = selected();
        if (c != null) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(link(c)), null);
        }
    }

    private void revokeSelected() {
        Certificate c = selected();
        if (c == null) {
            return;
        }
        if (c.isRevoked()) {
            JOptionPane.showMessageDialog(this, c.getCertNo() + " is already revoked.");
            return;
        }
        String reason = JOptionPane.showInputDialog(this,
                "<html>Revoke <b>" + c.getCertNo() + "</b> (" + Ui.html(c.getStudentName()) + ")?<br>"
                        + "This is permanent and sealed into the ledger.<br><br>Reason shown to verifiers:</html>",
                "Revoke certificate", JOptionPane.WARNING_MESSAGE);
        if (reason == null) {
            return;
        }
        Ui.background(this, () -> {
            service.revoke(c.getCertNo(), reason, account.username());
            return true;
        }, ok -> {
            JOptionPane.showMessageDialog(this, c.getCertNo() + " revoked.");
            refresh();
        });
    }
}
