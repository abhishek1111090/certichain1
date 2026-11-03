package com.certichain.desktop.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Plain JDBC connection factory (DriverManager, no pool, no framework).
 * Settings come from {@code desktop.properties} in the working directory, falling back
 * to the same H2 file the Spring Boot server uses, so both apps share one ledger.
 */
public final class Database {

    static {
        // Same as the server: share the H2 file over loopback, never the (changing) Wi-Fi address.
        System.setProperty("h2.bindAddress", "127.0.0.1");
    }

    private final String url;
    private final String user;
    private final String password;
    private final String institutionCode;
    private final String portalUrl;

    private Database(Properties p) {
        String defaultUrl = "jdbc:h2:file:" + System.getProperty("user.home").replace('\\', '/')
                + "/certichain-data/certichain;AUTO_SERVER=TRUE";
        this.url = p.getProperty("db.url", defaultUrl);
        this.user = p.getProperty("db.user", "sa");
        this.password = p.getProperty("db.password", "");
        this.institutionCode = p.getProperty("institution.code", "CIT");
        this.portalUrl = p.getProperty("portal.url", "http://localhost:8080");
    }

    public static Database load() {
        Properties p = new Properties();
        Path file = Path.of("desktop.properties");
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read desktop.properties", e);
            }
        }
        // -Ddb.url=... on the command line wins over the file.
        System.getProperties().stringPropertyNames().stream()
                .filter(k -> k.startsWith("db.") || k.startsWith("portal.") || k.startsWith("institution."))
                .forEach(k -> p.setProperty(k, System.getProperty(k)));
        return new Database(p);
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Creates the tables if they do not exist yet (same script the server runs). */
    public void initSchema() throws SQLException, IOException {
        try (InputStream in = Database.class.getResourceAsStream("/db/schema.sql")) {
            if (in == null) {
                throw new IOException("db/schema.sql missing from the classpath");
            }
            StringBuilder sql = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.trim().startsWith("--")) {
                        sql.append(line).append('\n');
                    }
                }
            }
            try (Connection c = connect(); Statement st = c.createStatement()) {
                for (String stmt : sql.toString().split(";")) {
                    if (!stmt.isBlank()) {
                        st.execute(stmt);
                    }
                }
            }
        }
    }

    public String getUrl() { return url; }
    public String getInstitutionCode() { return institutionCode; }
    public String getPortalUrl() { return portalUrl; }
}
