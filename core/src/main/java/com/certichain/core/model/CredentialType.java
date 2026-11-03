package com.certichain.core.model;

public enum CredentialType {
    DEGREE("Degree Certificate"),
    COURSE_COMPLETION("Course Completion"),
    INTERNSHIP("Internship Certificate"),
    HACKATHON("Hackathon / Competition Award"),
    WORKSHOP("Workshop Participation"),
    MERIT("Merit Certificate");

    private final String displayName;

    CredentialType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
