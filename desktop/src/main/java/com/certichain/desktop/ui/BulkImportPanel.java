package com.certichain.desktop.ui;

import com.certichain.core.model.Certificate;
import com.certichain.desktop.dao.AccountDao;
import com.certichain.desktop.service.CsvImporter;
import com.certichain.desktop.service.RegistrarService;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Convocation-day workflow: load a CSV of hundreds of students, preview it, and issue all
 * valid rows in a single transaction using JDBC batch inserts.
 */
public class BulkImportPanel extends JPanel {

    private final RegistrarService service;
    private final AccountDao.Account account;
    private final Runnable afterImport;
    private final CertificateTableModel preview = new CertificateTableModel();
    private final JTextArea problems = new JTextArea();
    private final JLabel summary = Ui.muted("Choose a CSV file to begin.");
    private final JProgressBar progress = new JProgressBar();
    private final JButton issueAll = new JButton("Issue all valid rows");
    private List<Certificate> pending = List.of();

    public BulkImportPanel(RegistrarService service, AccountDao.Account account, Runnable afterImport) {
        super(new BorderLayout(0, 12));
        this.service = service;
        this.account = account;
        this.afterImport = afterImport;
        setOpaque(false);

        JButton choose = new JButton("Choose CSV...");
        choose.addActionListener(e -> choose());
        JButton sample = new JButton("Save sample CSV...");
        sample.addActionListener(e -> saveSample());
        issueAll.setEnabled(false);
        issueAll.setBackground(Ui.BRAND);
        issueAll.setForeground(Color.WHITE);
        issueAll.addActionListener(e -> issueAll());
        progress.setVisible(false);
        progress.setIndeterminate(true);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(choose);
        bar.add(sample);
        bar.add(issueAll);
        bar.add(progress);
        bar.add(summary);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(Ui.header("Bulk import", "Columns: " + CsvImporter.HEADER), BorderLayout.NORTH);
        north.add(bar, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        JTable table = new JTable(preview);
        problems.setEditable(false);
        problems.setForeground(Ui.BAD);
        problems.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, Ui.card(new JScrollPane(table)), Ui.card(new JScrollPane(problems)));
        split.setResizeWeight(0.72);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);
    }

    private void choose() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File f = fc.getSelectedFile();
        Ui.background(this, () -> new CsvImporter().read(f.toPath()), result -> {
            pending = result.valid();
            preview.setRows(pending);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Integer, String> e : result.errors().entrySet()) {
                sb.append("Line ").append(e.getKey()).append(": ").append(e.getValue()).append('\n');
            }
            problems.setText(sb.length() == 0 ? "No problems found." : sb.toString());
            problems.setForeground(sb.length() == 0 ? Ui.OK : Ui.BAD);
            summary.setText(f.getName() + ": " + pending.size() + " valid, " + result.errors().size() + " rejected");
            issueAll.setEnabled(!pending.isEmpty());
            issueAll.setText("Issue " + pending.size() + " certificate(s)");
        });
    }

    private void saveSample() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("certichain-sample.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(fc.getSelectedFile().toPath(), CsvImporter.sample(), StandardCharsets.UTF_8);
                summary.setText("Sample saved to " + fc.getSelectedFile().getName());
            } catch (Exception ex) {
                Ui.error(this, ex);
            }
        }
    }

    private void issueAll() {
        if (JOptionPane.showConfirmDialog(this, "Issue " + pending.size() + " certificates in one transaction?",
                "Bulk issue", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        issueAll.setEnabled(false);
        progress.setVisible(true);
        long start = System.currentTimeMillis();
        Ui.background(this, () -> service.issueAll(pending, account.username()), issued -> {
            progress.setVisible(false);
            preview.setRows(issued);
            summary.setText(issued.size() + " certificates issued and sealed in " + (System.currentTimeMillis() - start) + " ms");
            summary.setForeground(Ui.OK);
            pending = List.of();
            issueAll.setText("Issue all valid rows");
            afterImport.run();
        });
    }
}
