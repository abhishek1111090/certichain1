package com.certichain.desktop.ui;

import com.certichain.core.model.Certificate;
import com.certichain.core.model.CredentialType;
import com.certichain.desktop.dao.AccountDao;
import com.certichain.desktop.service.RegistrarService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/** Form for issuing a single certificate. */
public class IssuePanel extends JPanel {

    private final RegistrarService service;
    private final AccountDao.Account account;
    private final Runnable afterIssue;

    private final JTextField name = new JTextField();
    private final JTextField roll = new JTextField();
    private final JTextField email = new JTextField();
    private final JComboBox<String> program = new JComboBox<>(new String[]{"B.Tech CSE", "B.Tech IT", "B.Tech CCE", "B.Tech ECE", "M.Tech CSE", "MCA"});
    private final JComboBox<CredentialType> type = new JComboBox<>(CredentialType.values());
    private final JTextField title = new JTextField();
    private final JTextField grade = new JTextField();
    private final JSpinner date = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));

    public IssuePanel(RegistrarService service, AccountDao.Account account, Runnable afterIssue) {
        super(new BorderLayout());
        this.service = service;
        this.account = account;
        this.afterIssue = afterIssue;
        setOpaque(false);
        program.setEditable(true);
        date.setEditor(new JSpinner.DateEditor(date, "yyyy-MM-dd"));
        title.putClientProperty("JTextField.placeholderText", "e.g. Advanced Java Programming (INT3140)");
        grade.putClientProperty("JTextField.placeholderText", "A+, 8.9 CGPA, 1st Prize");

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int row = 0;
        row = addRow(form, row, "Student name *", name, "Roll number *", roll);
        row = addRow(form, row, "Student email", email, "Program *", program);
        row = addRow(form, row, "Credential type *", type, "Issue date *", date);
        row = addRow(form, row, "Title *", title, "Grade / result", grade);

        JButton issue = new JButton("Issue & seal into ledger");
        issue.setBackground(Ui.BRAND);
        issue.setForeground(Color.WHITE);
        issue.addActionListener(e -> issue());
        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> clear());
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        buttons.add(issue);
        buttons.add(javax.swing.Box.createHorizontalStrut(8));
        buttons.add(clear);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = row;
        g.gridwidth = 2;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(16, 0, 0, 0);
        form.add(buttons, g);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(form, BorderLayout.NORTH);
        JLabel note = Ui.muted("<html>The row and its ledger block are written in one JDBC transaction "
                + "(setAutoCommit(false) &rarr; commit / rollback). Issued certificates can never be edited, only revoked.</html>");
        note.setBorder(Ui.pad(4));
        wrap.add(note, BorderLayout.SOUTH);

        add(Ui.header("Issue certificate", "Creates a certificate and appends an ISSUE block to the ledger"), BorderLayout.NORTH);
        add(Ui.card(wrap), BorderLayout.CENTER);
    }

    private static int addRow(JPanel p, int row, String l1, JComponent c1, String l2, JComponent c2) {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.insets = new Insets(4, 0, 2, 16);
        g.gridy = row;
        g.gridx = 0;
        p.add(new JLabel(l1), g);
        g.gridx = 1;
        p.add(new JLabel(l2), g);
        g.gridy = row + 1;
        g.insets = new Insets(0, 0, 10, 16);
        g.gridx = 0;
        p.add(c1, g);
        g.gridx = 1;
        p.add(c2, g);
        return row + 2;
    }

    private void issue() {
        Certificate c = new Certificate();
        c.setStudentName(name.getText());
        c.setRollNo(roll.getText());
        c.setEmail(email.getText());
        c.setProgram(String.valueOf(program.getSelectedItem()));
        c.setCredentialType((CredentialType) type.getSelectedItem());
        c.setTitle(title.getText());
        c.setGrade(grade.getText());
        c.setIssueDate(((Date) date.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        Ui.background(this, () -> service.issue(c, account.username()), issued -> {
            String link = service.getDatabase().getPortalUrl() + "/verify/" + issued.getCertNo();
            Object[] options = {"Copy verification link", "Close"};
            int choice = JOptionPane.showOptionDialog(this,
                    "<html><b>" + issued.getCertNo() + "</b> issued to " + Ui.html(issued.getStudentName())
                            + ".<br><br>SHA-256: <code>" + issued.getDataHash().substring(0, 32) + "...</code><br>Verify at: " + link + "</html>",
                    "Certificate issued", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
            if (choice == 0) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(link), null);
            }
            clear();
            afterIssue.run();
        });
    }

    private void clear() {
        name.setText("");
        roll.setText("");
        email.setText("");
        title.setText("");
        grade.setText("");
        date.setValue(new Date());
        name.requestFocusInWindow();
    }
}
