package com.certichain.core.util;

import com.certichain.core.exception.InvalidCertificateException;
import com.certichain.core.model.Certificate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class CertificateValidator {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern ROLL = Pattern.compile("^[A-Za-z0-9/-]{3,30}$");

    private CertificateValidator() {
    }

    public static void validate(Certificate c) {
        List<String> errors = new ArrayList<>();
        if (blank(c.getStudentName()) || c.getStudentName().trim().length() < 3) {
            errors.add("Student name must have at least 3 characters");
        }
        if (blank(c.getRollNo()) || !ROLL.matcher(c.getRollNo().trim()).matches()) {
            errors.add("Roll number must be 3-30 letters, digits, '/' or '-'");
        }
        if (!blank(c.getEmail()) && !EMAIL.matcher(c.getEmail().trim()).matches()) {
            errors.add("Email address is not valid");
        }
        if (blank(c.getProgram())) {
            errors.add("Program is required");
        }
        if (c.getCredentialType() == null) {
            errors.add("Credential type is required");
        }
        if (blank(c.getTitle())) {
            errors.add("Title is required");
        }
        if (c.getIssueDate() == null) {
            errors.add("Issue date is required");
        } else if (c.getIssueDate().isAfter(LocalDate.now())) {
            errors.add("Issue date cannot be in the future");
        }
        if (!errors.isEmpty()) {
            throw new InvalidCertificateException(errors);
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
