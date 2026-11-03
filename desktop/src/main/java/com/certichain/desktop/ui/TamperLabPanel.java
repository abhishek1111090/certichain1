package com.certichain.desktop.ui;

import com.certichain.core.chain.IntegrityReport;
import com.certichain.core.model.Certificate;
import com.certichain.desktop.dao.AccountDao;
import com.certichain.desktop.service.RegistrarService;
import com.certichain.desktop.service.TamperLab;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Live demonstration for the viva: act as a corrupt insider with direct database access,
 * then watch the audit (and the public web portal) catch every change.
 */
public class TamperLabPanel extends JPanel implements Refreshable {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final RegistrarService service;
    private final AccountDao.Account account;
    private final TamperLab lab;
    private final JComboBox<Certificate> target = new JComboBox<>();
    private final JComboBox<TamperLab.Attack> attack = new JComboBox<>(TamperLab.Attack.values());
    private final JTextArea log = new JTextArea();
    private final JButton undo = new JButton("Undo all attacks");

    public TamperLabPanel(RegistrarService service, AccountDao.Account account) {
        super(new BorderLayout(0, 12));
        this.service = service;
        this.account = account;
        this.lab = new TamperLab(service.getDatabase());
        setOpaque(false);

        JTextArea warning = new JTextArea("Security demonstration. These buttons run raw SQL UPDATE / INSERT statements "
                + "against the database, bypassing CertiChain entirely, exactly like a dishonest DBA would. "
                + "Use \"Undo all attacks\" afterwards to restore the demo data.");
        warning.setEditable(false);
        warning.setLineWrap(true);
        warning.setWrapStyleWord(true);
        warning.setFont(Ui.font(Font.BOLD, 12.5f));
        warning.setOpaque(true);
        warning.setBackground(Ui.WARN_SOFT);
        warning.setForeground(Ui.WARN);
        warning.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xF6D39A)), Ui.pad(10)));

        JButton perform = new JButton("Perform attack");
        perform.setBackground(Ui.BAD);
        perform.setForeground(Color.WHITE);
        perform.addActionListener(e -> perform());
        JButton audit = new JButton("Run integrity audit");
        audit.addActionListener(e -> audit());
        undo.addActionListener(e -> undoAll());
        undo.setEnabled(false);

        JPanel form = new JPanel(new GridLayout(2, 2, 12, 4));
        form.setOpaque(false);
        form.add(new JLabel("Target certificate"));
        form.add(new JLabel("Attack"));
        form.add(target);
        form.add(attack);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        buttons.setOpaque(false);
        buttons.add(perform);
        buttons.add(audit);
        buttons.add(undo);

        JPanel controls = new JPanel(new BorderLayout(0, 10));
        controls.setOpaque(false);
        controls.add(warning, BorderLayout.NORTH);
        controls.add(form, BorderLayout.CENTER);
        controls.add(buttons, BorderLayout.SOUTH);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(Ui.header("Tamper lab", "Prove the ledger catches insider fraud"), BorderLayout.NORTH);
        north.add(Ui.card(controls), BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setText("Pick a certificate and an attack, perform it, then run the audit or open the certificate's\n"
                + "verification page in the browser: it now shows TAMPERED.\n\n");
        add(Ui.card(new JScrollPane(log)), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Ui.background(this, service::allCertificates, certs -> {
            Object keep = target.getSelectedItem();
            target.setModel(new DefaultComboBoxModel<>(certs.toArray(new Certificate[0])));
            if (keep != null) {
                target.setSelectedItem(keep);
            }
        });
    }

    private void perform() {
        Certificate c = (Certificate) target.getSelectedItem();
        TamperLab.Attack a = (TamperLab.Attack) attack.getSelectedItem();
        if (c == null || a == null) {
            return;
        }
        Ui.background(this, () -> lab.perform(a, c), what -> {
            append("ATTACK  " + what);
            append("        verify: " + service.getDatabase().getPortalUrl() + "/verify/"
                    + (a == TamperLab.Attack.FORGE_CERTIFICATE ? what.split(" ")[3] : c.getCertNo()));
            undo.setEnabled(true);
            undo.setText("Undo all attacks (" + lab.pendingUndos() + ")");
            refresh();
        });
    }

    private void audit() {
        Ui.background(this, () -> service.audit(account.username()), (IntegrityReport r) -> {
            append("AUDIT   " + r.summary() + " (" + r.getDurationMs() + " ms, " + r.getThreadsUsed() + " threads)");
            r.getIssues().forEach(i -> append("        - " + i.type() + " " + (i.certNo() == null ? "" : i.certNo()) + ": " + i.detail()));
        });
    }

    private void undoAll() {
        Ui.background(this, lab::undoAll, n -> {
            append("UNDO    restored " + n + " change(s); run the audit again to confirm the ledger is intact");
            undo.setEnabled(false);
            undo.setText("Undo all attacks");
            refresh();
        });
    }

    private void append(String line) {
        log.append("[" + LocalTime.now().format(TIME) + "] " + line + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }
}
