package com.certichain.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plain Java bean defined in {@code beans.xml}. Spring calls {@link #init()} after all
 * properties are injected and {@link #shutdown()} when the context closes.
 */
public class InstitutionProfile {

    private static final Logger log = LoggerFactory.getLogger(InstitutionProfile.class);

    private String name;
    private String shortCode;
    private String address;
    private String registrarName;
    private String registrarTitle;
    private String website;

    public void init() {
        if (shortCode == null || !shortCode.matches("[A-Z]{2,6}")) {
            throw new IllegalStateException("institutionProfile.shortCode must be 2-6 capital letters");
        }
        log.info("Bean life-cycle: institutionProfile initialised for {} ({})", name, shortCode);
    }

    public void shutdown() {
        log.info("Bean life-cycle: institutionProfile destroyed");
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getRegistrarName() { return registrarName; }
    public void setRegistrarName(String registrarName) { this.registrarName = registrarName; }
    public String getRegistrarTitle() { return registrarTitle; }
    public void setRegistrarTitle(String registrarTitle) { this.registrarTitle = registrarTitle; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}
