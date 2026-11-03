package com.certichain.desktop.service;

import com.certichain.core.exception.InvalidCertificateException;
import com.certichain.core.model.Certificate;
import com.certichain.core.model.CredentialType;
import com.certichain.core.util.CertificateValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads a convocation list in CSV form. Every line is validated; lines with problems are
 * reported with their line number instead of aborting the whole file.
 */
public class CsvImporter {

    public static final String HEADER = "student_name,roll_no,email,program,credential_type,title,grade,issue_date";

    public record Result(List<Certificate> valid, Map<Integer, String> errors) {
    }

    public Result read(Path file) throws IOException {
        List<Certificate> valid = new ArrayList<>();
        Map<Integer, String> errors = new LinkedHashMap<>();
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line = r.readLine();
            if (line == null) {
                return new Result(valid, errors);
            }
            if (line.startsWith("﻿")) {
                line = line.substring(1);
            }
            if (!line.trim().equalsIgnoreCase(HEADER)) {
                errors.put(1, "Header must be: " + HEADER);
                return new Result(valid, errors);
            }
            int n = 1;
            while ((line = r.readLine()) != null) {
                n++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    Certificate c = toCertificate(split(line));
                    CertificateValidator.validate(c);
                    valid.add(c);
                } catch (InvalidCertificateException e) {
                    errors.put(n, String.join("; ", e.getErrors()));
                } catch (IllegalArgumentException | DateTimeParseException e) {
                    errors.put(n, e.getMessage());
                }
            }
        }
        return new Result(valid, errors);
    }

    private static Certificate toCertificate(List<String> f) {
        if (f.size() != 8) {
            throw new IllegalArgumentException("Expected 8 columns, found " + f.size());
        }
        Certificate c = new Certificate();
        c.setStudentName(f.get(0));
        c.setRollNo(f.get(1));
        c.setEmail(f.get(2));
        c.setProgram(f.get(3));
        try {
            c.setCredentialType(CredentialType.valueOf(f.get(4).trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown credential_type '" + f.get(4) + "'");
        }
        c.setTitle(f.get(5));
        c.setGrade(f.get(6));
        c.setIssueDate(f.get(7).isBlank() ? LocalDate.now() : LocalDate.parse(f.get(7).trim()));
        return c;
    }

    /** RFC 4180 field splitting: commas inside "quoted, values" and "" escapes. */
    static List<String> split(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else if (ch == '"') {
                    quoted = false;
                } else {
                    cur.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out;
    }

    public static String sample() {
        return HEADER + "\n"
                + "Priya Sharma,22CS2001,priya.sharma@student.example,B.Tech CSE,COURSE_COMPLETION,Advanced Java Programming (INT3140),A+," + LocalDate.now() + "\n"
                + "Rahul Khanna,22CS2002,rahul.khanna@student.example,B.Tech CSE,COURSE_COMPLETION,Advanced Java Programming (INT3140),A," + LocalDate.now() + "\n"
                + "Neha Patel,22IT2003,,B.Tech IT,WORKSHOP,\"Workshop on JDBC, Servlets and JSP\",," + LocalDate.now() + "\n"
                + "Sameer Ali,22CS2004,sameer.ali@student.example,B.Tech CSE,HACKATHON,Winner - Code Sprint 2026,1st Prize," + LocalDate.now() + "\n"
                + "Bad Row,!!,not-an-email,B.Tech CSE,UNKNOWN_TYPE,Something,," + LocalDate.now() + "\n";
    }
}
