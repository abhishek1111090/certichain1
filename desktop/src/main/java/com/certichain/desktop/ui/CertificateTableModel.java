package com.certichain.desktop.ui;

import com.certichain.core.model.Certificate;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Adapts a List&lt;Certificate&gt; to JTable. */
public class CertificateTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Certificate no", "Student", "Roll no", "Title", "Type", "Grade", "Issued", "Status"};

    private List<Certificate> rows = new ArrayList<>();

    public void setRows(List<Certificate> rows) {
        this.rows = new ArrayList<>(rows);
        fireTableDataChanged();
    }

    public Certificate get(int modelRow) {
        return rows.get(modelRow);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int row, int col) {
        Certificate c = rows.get(row);
        return switch (col) {
            case 0 -> c.getCertNo();
            case 1 -> c.getStudentName();
            case 2 -> c.getRollNo();
            case 3 -> c.getTitle();
            case 4 -> c.getCredentialType().getDisplayName();
            case 5 -> c.getGrade() == null ? "" : c.getGrade();
            case 6 -> c.getIssueDate().toString();
            case 7 -> c.getStatus().name();
            default -> "";
        };
    }
}
