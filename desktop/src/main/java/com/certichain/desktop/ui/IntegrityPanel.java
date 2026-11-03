package com.certichain.desktop.ui;

import com.certichain.core.chain.IntegrityIssue;
import com.certichain.core.chain.IntegrityReport;
import com.certichain.desktop.dao.AccountDao;
import com.certichain.desktop.service.RegistrarService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

/** Runs the multithreaded full-chain audit and lists every finding. */
public class IntegrityPanel extends JPanel implements Refreshable {

    private final RegistrarService service;
    private final AccountDao.Account account;
    private final JLabel verdict = new JLabel("Not run yet");
    private final JLabel stats = Ui.muted(" ");
    private final DefaultTableModel findings = new DefaultTableModel(new String[]{"Block", "Finding", "Certificate", "Detail"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public IntegrityPanel(RegistrarService service, AccountDao.Account account) {
        super(new BorderLayout(0, 12));
        this.service = service;
        this.account = account;
        setOpaque(false);

        JButton run = new JButton("Run full audit");
        run.setBackground(Ui.BRAND);
        run.setForeground(Color.WHITE);
        run.addActionListener(e -> run());
        verdict.setFont(Ui.font(Font.BOLD, 18f));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        bar.setOpaque(false);
        bar.add(run);
        bar.add(verdict);
        bar.add(stats);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(Ui.header("Integrity audit", "Re-hashes every block and certificate on a thread pool (ExecutorService + Callable/Future)"),
                BorderLayout.NORTH);
        north.add(bar, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        JTable table = new JTable(findings);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(420);
        table.getColumnModel().getColumn(1).setCellRenderer(new StatusRenderer());
        add(Ui.card(new JScrollPane(table)), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        run();
    }

    void run() {
        verdict.setText("Auditing...");
        verdict.setForeground(Ui.MUTED);
        Ui.background(this, () -> service.audit(account.username()), this::show);
    }

    private void show(IntegrityReport r) {
        verdict.setText(r.isIntact() ? "Ledger intact" : "Breach: " + r.getIssueCount() + " finding(s)");
        verdict.setForeground(r.isIntact() ? Ui.OK : Ui.BAD);
        stats.setText(r.getBlocksChecked() + " blocks, " + r.getCertificatesChecked() + " certificates, "
                + r.getThreadsUsed() + " threads, " + r.getDurationMs() + " ms");
        findings.setRowCount(0);
        for (IntegrityIssue i : r.getIssues()) {
            findings.addRow(new Object[]{i.seq() < 0 ? "-" : i.seq(), i.type().name(), i.certNo(), i.detail()});
        }
    }
}
