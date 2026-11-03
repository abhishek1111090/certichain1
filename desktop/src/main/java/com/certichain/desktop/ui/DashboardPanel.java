package com.certichain.desktop.ui;

import com.certichain.core.model.Certificate;
import com.certichain.desktop.dao.LedgerDao;
import com.certichain.desktop.service.RegistrarService;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DashboardPanel extends JPanel implements Refreshable {

    private record Snapshot(List<Certificate> certs, LedgerDao.Head head) {
    }

    private final RegistrarService service;
    private final JLabel total = value();
    private final JLabel valid = value();
    private final JLabel revoked = value();
    private final JLabel blocks = value();
    private final JLabel headHash = Ui.muted(" ");
    private final JLabel byType = Ui.muted(" ");
    private final CertificateTableModel recent = new CertificateTableModel();

    public DashboardPanel(RegistrarService service) {
        super(new BorderLayout(0, 16));
        this.service = service;
        setOpaque(false);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Ui.header("Dashboard", "Live view of the shared ledger database"), BorderLayout.NORTH);
        JPanel tiles = new JPanel(new GridLayout(1, 4, 14, 0));
        tiles.setOpaque(false);
        tiles.add(tile("Certificates", total, Ui.BRAND));
        tiles.add(tile("Valid", valid, Ui.OK));
        tiles.add(tile("Revoked", revoked, Ui.WARN));
        tiles.add(tile("Ledger blocks", blocks, Ui.BRAND));
        top.add(tiles, BorderLayout.CENTER);
        JPanel info = new JPanel(new GridLayout(2, 1));
        info.setOpaque(false);
        info.add(headHash);
        info.add(byType);
        info.setBorder(Ui.pad(6));
        top.add(info, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JTable table = new JTable(recent);
        table.getColumnModel().getColumn(7).setCellRenderer(new StatusRenderer());
        JPanel recentPanel = new JPanel(new BorderLayout(0, 8));
        recentPanel.setOpaque(false);
        JLabel h = new JLabel("Most recent certificates");
        h.setFont(Ui.font(Font.BOLD, 14f));
        recentPanel.add(h, BorderLayout.NORTH);
        recentPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        add(Ui.card(recentPanel), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Ui.background(this, () -> new Snapshot(service.allCertificates(), service.head()), s -> {
            long rev = s.certs().stream().filter(Certificate::isRevoked).count();
            total.setText(String.valueOf(s.certs().size()));
            valid.setText(String.valueOf(s.certs().size() - rev));
            revoked.setText(String.valueOf(rev));
            blocks.setText(String.valueOf(s.head().seq()));
            headHash.setText("Ledger head hash: " + s.head().hash());
            Map<com.certichain.core.model.CredentialType, Integer> counts =
                    new EnumMap<>(com.certichain.core.model.CredentialType.class);
            s.certs().forEach(c -> counts.merge(c.getCredentialType(), 1, Integer::sum));
            StringBuilder sb = new StringBuilder("By type:  ");
            counts.forEach((t, n) -> sb.append(t.getDisplayName()).append(" ").append(n).append("    "));
            byType.setText(sb.toString());
            recent.setRows(s.certs().subList(0, Math.min(12, s.certs().size())));
        });
    }

    private static JLabel value() {
        JLabel l = new JLabel("-");
        l.setFont(Ui.font(Font.BOLD, 28f));
        return l;
    }

    private static JPanel tile(String label, JLabel value, Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = Ui.muted(label.toUpperCase());
        l.setFont(Ui.font(Font.BOLD, 11f));
        value.setForeground(accent);
        p.add(l, BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return Ui.card(p);
    }
}
