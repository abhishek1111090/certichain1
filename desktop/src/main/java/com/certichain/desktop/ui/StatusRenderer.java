package com.certichain.desktop.ui;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.Font;

/** Colours status / result cells (VALID, REVOKED, TAMPERED...) like badges. */
public class StatusRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, col);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        l.setHorizontalAlignment(CENTER);
        if (!selected) {
            String v = String.valueOf(value);
            if (v.equals("VALID") || v.equals("ISSUE")) {
                l.setForeground(Ui.OK);
                l.setBackground(Ui.OK_SOFT);
            } else if (v.equals("REVOKED") || v.equals("REVOKE")) {
                l.setForeground(Ui.WARN);
                l.setBackground(Ui.WARN_SOFT);
            } else {
                l.setForeground(Ui.BAD);
                l.setBackground(Ui.BAD_SOFT);
            }
        }
        return l;
    }
}
