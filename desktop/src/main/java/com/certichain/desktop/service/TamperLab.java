package com.certichain.desktop.service;

import com.certichain.core.model.Certificate;
import com.certichain.core.model.CredentialType;
import com.certichain.desktop.db.Database;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Security demonstration: plays the part of a dishonest insider who edits the database
 * directly with SQL, bypassing the application and the ledger. Each attack pushes its
 * own undo step onto a stack so the demo database can be restored afterwards.
 */
public class TamperLab {

    public enum Attack {
        CHANGE_GRADE("Upgrade the grade of a certificate"),
        CHANGE_NAME("Swap the student name (sell the certificate)"),
        HIDE_REVOCATION("Flip a revoked certificate back to VALID"),
        FORGE_CERTIFICATE("Insert a forged certificate row directly");

        private final String label;

        Attack(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @FunctionalInterface
    private interface SqlStep {
        void run(Connection c) throws SQLException;
    }

    private record Undo(String description, SqlStep step) {
    }

    private final Database db;
    private final Deque<Undo> undoStack = new ArrayDeque<>();

    public TamperLab(Database db) {
        this.db = db;
    }

    /** @return a description of what was changed */
    public String perform(Attack attack, Certificate target) throws SQLException {
        try (Connection c = db.connect()) {
            switch (attack) {
                case CHANGE_GRADE -> {
                    String old = target.getGrade();
                    update(c, "UPDATE certificate SET grade = ? WHERE cert_no = ?", "O (Outstanding)", target.getCertNo());
                    undoStack.push(new Undo("restore grade of " + target.getCertNo(),
                            k -> update(k, "UPDATE certificate SET grade = ? WHERE cert_no = ?", old, target.getCertNo())));
                    return "Grade of " + target.getCertNo() + " changed from '" + old + "' to 'O (Outstanding)'";
                }
                case CHANGE_NAME -> {
                    String old = target.getStudentName();
                    update(c, "UPDATE certificate SET student_name = ? WHERE cert_no = ?", "Mallory Impostor", target.getCertNo());
                    undoStack.push(new Undo("restore name of " + target.getCertNo(),
                            k -> update(k, "UPDATE certificate SET student_name = ? WHERE cert_no = ?", old, target.getCertNo())));
                    return "Name on " + target.getCertNo() + " changed from '" + old + "' to 'Mallory Impostor'";
                }
                case HIDE_REVOCATION -> {
                    if (!target.isRevoked()) {
                        throw new IllegalArgumentException("Pick a REVOKED certificate for this attack");
                    }
                    update(c, "UPDATE certificate SET status = 'VALID' WHERE cert_no = ?", target.getCertNo());
                    undoStack.push(new Undo("re-revoke " + target.getCertNo(),
                            k -> update(k, "UPDATE certificate SET status = 'REVOKED' WHERE cert_no = ?", target.getCertNo())));
                    return target.getCertNo() + " now says VALID in the database";
                }
                case FORGE_CERTIFICATE -> {
                    Certificate f = new Certificate();
                    f.setCertNo(db.getInstitutionCode() + "-" + LocalDate.now().getYear() + "-FORGED01");
                    f.setStudentName("Mallory Impostor");
                    f.setRollNo("22CS6666");
                    f.setProgram("B.Tech CSE");
                    f.setCredentialType(CredentialType.DEGREE);
                    f.setTitle("Bachelor of Technology in Computer Science and Engineering");
                    f.setGrade("9.9 CGPA");
                    f.setIssueDate(LocalDate.now().minusDays(30));
                    f.setIssuedBy("admin");
                    // The attacker even computes a correct-looking hash, but cannot add a ledger block.
                    f.setDataHash(f.computeHash());
                    try (PreparedStatement ps = c.prepareStatement("""
                            INSERT INTO certificate (cert_no, student_name, roll_no, program, credential_type, title, grade,
                                                     issue_date, issued_by, status, data_hash, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'VALID', ?, ?)""")) {
                        ps.setString(1, f.getCertNo());
                        ps.setString(2, f.getStudentName());
                        ps.setString(3, f.getRollNo());
                        ps.setString(4, f.getProgram());
                        ps.setString(5, f.getCredentialType().name());
                        ps.setString(6, f.getTitle());
                        ps.setString(7, f.getGrade());
                        ps.setDate(8, Date.valueOf(f.getIssueDate()));
                        ps.setString(9, f.getIssuedBy());
                        ps.setString(10, f.getDataHash());
                        ps.setLong(11, System.currentTimeMillis());
                        ps.executeUpdate();
                    }
                    undoStack.push(new Undo("delete forged " + f.getCertNo(),
                            k -> update(k, "DELETE FROM certificate WHERE cert_no = ?", f.getCertNo())));
                    return "Inserted forged degree " + f.getCertNo() + " for 'Mallory Impostor' (9.9 CGPA)";
                }
                default -> throw new IllegalStateException();
            }
        }
    }

    /** Undoes every attack, most recent first. @return how many were undone */
    public int undoAll() throws SQLException {
        int n = 0;
        try (Connection c = db.connect()) {
            while (!undoStack.isEmpty()) {
                undoStack.pop().step().run(c);
                n++;
            }
        }
        return n;
    }

    public int pendingUndos() {
        return undoStack.size();
    }

    private static void update(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.executeUpdate();
        }
    }
}
