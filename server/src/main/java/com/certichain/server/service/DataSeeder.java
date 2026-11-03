package com.certichain.server.service;

import com.certichain.core.crypto.PasswordHasher;
import com.certichain.core.model.Certificate;
import com.certichain.core.model.CredentialType;
import com.certichain.server.model.Role;
import com.certichain.server.repository.LedgerRepository;
import com.certichain.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** First-run setup: default accounts and, optionally, a realistic set of demo certificates. */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final LedgerRepository ledger;
    private final CertificateService certificateService;
    private final boolean seedDemo;
    private final String adminPassword;
    private final String registrarPassword;

    public DataSeeder(UserRepository users, LedgerRepository ledger, CertificateService certificateService,
                      @Value("${certichain.seed-demo:true}") boolean seedDemo,
                      @Value("${certichain.admin-password:admin123}") String adminPassword,
                      @Value("${certichain.registrar-password:registrar123}") String registrarPassword) {
        this.users = users;
        this.ledger = ledger;
        this.certificateService = certificateService;
        this.seedDemo = seedDemo;
        this.adminPassword = adminPassword;
        this.registrarPassword = registrarPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() == 0) {
            users.insert("admin", "Dr. Anita Sharma", PasswordHasher.hash(adminPassword.toCharArray()), Role.ADMIN);
            users.insert("registrar", "Rohit Verma", PasswordHasher.hash(registrarPassword.toCharArray()), Role.STAFF);
            log.info("Created the default accounts 'admin' and 'registrar'");
        }
        if (seedDemo && ledger.count() == 0) {
            seedCertificates();
        }
    }

    private void seedCertificates() {
        record Demo(String name, String roll, String program, CredentialType type, String title, String grade, int daysAgo) {
        }
        List<Demo> demos = List.of(
                new Demo("Aarav Mehta", "22CS1001", "B.Tech CSE", CredentialType.DEGREE, "Bachelor of Technology in Computer Science and Engineering", "8.92 CGPA", 40),
                new Demo("Diya Kapoor", "22CS1002", "B.Tech CSE", CredentialType.COURSE_COMPLETION, "Advanced Java Programming (INT3140)", "A+", 35),
                new Demo("Kabir Singh", "22IT1044", "B.Tech IT", CredentialType.INTERNSHIP, "Summer Internship - Backend Engineering", "Excellent", 30),
                new Demo("Ananya Iyer", "22CS1078", "B.Tech CSE", CredentialType.HACKATHON, "Winner - Smart Campus Hackathon 2026", "1st Prize", 27),
                new Demo("Vihaan Gupta", "22CS1103", "B.Tech CSE", CredentialType.COURSE_COMPLETION, "Advanced Java Programming (INT3140)", "A", 25),
                new Demo("Ishita Rao", "22CC1012", "B.Tech CCE", CredentialType.WORKSHOP, "Workshop on Spring Boot Microservices", null, 21),
                new Demo("Arjun Nair", "22CS1150", "B.Tech CSE", CredentialType.MERIT, "Dean's List - Semester V", "Top 5%", 18),
                new Demo("Saanvi Joshi", "22IT1201", "B.Tech IT", CredentialType.COURSE_COMPLETION, "Cloud Computing Fundamentals", "A", 14),
                new Demo("Reyansh Malhotra", "22CS1222", "B.Tech CSE", CredentialType.INTERNSHIP, "Research Internship - Applied Cryptography", "Outstanding", 10),
                new Demo("Myra Choudhary", "22CS1240", "B.Tech CSE", CredentialType.HACKATHON, "Runner-up - CTF Security Challenge", "2nd Prize", 7),
                new Demo("Advik Bansal", "22CS1301", "B.Tech CSE", CredentialType.COURSE_COMPLETION, "Advanced Java Programming (INT3140)", "B+", 4),
                new Demo("Kiara Desai", "22IT1333", "B.Tech IT", CredentialType.WORKSHOP, "Workshop on Ethical Hacking", null, 2));
        Certificate toRevoke = null;
        for (Demo d : demos) {
            Certificate c = new Certificate();
            c.setStudentName(d.name());
            c.setRollNo(d.roll());
            c.setEmail(d.name().toLowerCase().replace(' ', '.') + "@student.certichain.example");
            c.setProgram(d.program());
            c.setCredentialType(d.type());
            c.setTitle(d.title());
            c.setGrade(d.grade());
            c.setIssueDate(LocalDate.now().minusDays(d.daysAgo()));
            Certificate issued = certificateService.issue(c, "admin");
            if (d.roll().equals("22CS1103")) {
                toRevoke = issued;
            }
        }
        if (toRevoke != null) {
            certificateService.revoke(toRevoke.getCertNo(), "Re-evaluation changed the final grade; corrected certificate to be re-issued", "admin");
        }
        log.info("Seeded {} demo certificates into the ledger", demos.size());
    }
}
