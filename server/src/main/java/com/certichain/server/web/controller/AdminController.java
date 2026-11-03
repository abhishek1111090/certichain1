package com.certichain.server.web.controller;

import com.certichain.core.chain.IntegrityReport;
import com.certichain.core.exception.CertiChainException;
import com.certichain.core.exception.InvalidCertificateException;
import com.certichain.core.model.Certificate;
import com.certichain.core.model.CertificateStatus;
import com.certichain.core.model.CredentialType;
import com.certichain.server.config.InstitutionProfile;
import com.certichain.server.model.AdminSession;
import com.certichain.server.repository.AuditRepository;
import com.certichain.server.repository.LedgerRepository;
import com.certichain.server.repository.VerificationLogRepository;
import com.certichain.server.service.CertificateService;
import com.certichain.server.service.IntegrityService;
import com.certichain.server.service.StatsService;
import com.certichain.server.service.VerificationService;
import com.certichain.server.web.listener.SessionTracker;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** Back-office pages. Access control is done by AdminSecurityFilter before any of this runs. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CertificateService certificateService;
    private final VerificationService verificationService;
    private final IntegrityService integrityService;
    private final StatsService statsService;
    private final LedgerRepository ledger;
    private final AuditRepository audit;
    private final VerificationLogRepository verificationLog;
    private final InstitutionProfile institution;

    public AdminController(CertificateService certificateService, VerificationService verificationService,
                           IntegrityService integrityService, StatsService statsService, LedgerRepository ledger,
                           AuditRepository audit, VerificationLogRepository verificationLog, InstitutionProfile institution) {
        this.certificateService = certificateService;
        this.verificationService = verificationService;
        this.integrityService = integrityService;
        this.statsService = statsService;
        this.ledger = ledger;
        this.audit = audit;
        this.verificationLog = verificationLog;
        this.institution = institution;
    }

    @ModelAttribute
    public void common(Model model) {
        model.addAttribute("institution", institution);
        model.addAttribute("integrity", integrityService.getLastReport());
        model.addAttribute("credentialTypes", CredentialType.values());
    }

    @GetMapping({"", "/"})
    public String root() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("nav", "dashboard");
        model.addAttribute("stats", statsService.dashboard());
        model.addAttribute("recentBlocks", ledger.findLatest(8));
        model.addAttribute("suspicious", verificationLog.findSuspicious(6));
        model.addAttribute("activeSessions", SessionTracker.activeSessions());
        return "admin/dashboard";
    }

    @GetMapping("/certificates")
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) String status,
                       @RequestParam(required = false) String type, Model model) {
        model.addAttribute("nav", "certificates");
        model.addAttribute("certificates", certificateService.search(q, status, type));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("statuses", CertificateStatus.values());
        return "admin/certificates";
    }

    @GetMapping("/certificates/new")
    public String newForm(Model model) {
        model.addAttribute("nav", "issue");
        Certificate draft = new Certificate();
        draft.setIssueDate(LocalDate.now());
        model.addAttribute("form", draft);
        return "admin/issue";
    }

    @PostMapping("/certificates")
    public String issue(@RequestParam String studentName, @RequestParam String rollNo,
                        @RequestParam(required = false) String email, @RequestParam String program,
                        @RequestParam(required = false) CredentialType credentialType, @RequestParam String title,
                        @RequestParam(required = false) String grade,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
                        @SessionAttribute(AdminSession.ATTR) AdminSession admin,
                        Model model, RedirectAttributes flash) {
        Certificate draft = new Certificate();
        draft.setStudentName(studentName);
        draft.setRollNo(rollNo);
        draft.setEmail(email);
        draft.setProgram(program);
        draft.setCredentialType(credentialType);
        draft.setTitle(title);
        draft.setGrade(grade);
        draft.setIssueDate(issueDate);
        try {
            Certificate issued = certificateService.issue(draft, admin.getUsername());
            flash.addFlashAttribute("success", "Certificate " + issued.getCertNo() + " issued and sealed into the ledger.");
            return "redirect:/admin/certificates/" + issued.getCertNo();
        } catch (InvalidCertificateException e) {
            model.addAttribute("nav", "issue");
            model.addAttribute("form", draft);
            model.addAttribute("errors", e.getErrors());
            return "admin/issue";
        }
    }

    @GetMapping("/certificates/{certNo}")
    public String detail(@PathVariable String certNo, HttpServletRequest request, Model model) {
        model.addAttribute("nav", "certificates");
        Certificate c = certificateService.get(certNo);
        model.addAttribute("c", c);
        model.addAttribute("result", verificationService.verify(certNo, VerificationService.Channel.ADMIN,
                request.getRemoteAddr(), null));
        model.addAttribute("blocks", ledger.findByCertNo(certNo));
        model.addAttribute("lookups", verificationLog.findByCertNo(certNo, 20));
        return "admin/detail";
    }

    @PostMapping("/certificates/{certNo}/revoke")
    public String revoke(@PathVariable String certNo, @RequestParam String reason,
                         @SessionAttribute(AdminSession.ATTR) AdminSession admin, RedirectAttributes flash) {
        if (!admin.isAdmin()) {
            flash.addFlashAttribute("error", "Only administrators can revoke certificates.");
        } else {
            try {
                certificateService.revoke(certNo, reason, admin.getUsername());
                flash.addFlashAttribute("success", certNo + " revoked. The revocation is sealed in the ledger.");
            } catch (CertiChainException e) {
                flash.addFlashAttribute("error", e.getMessage());
            }
        }
        return "redirect:/admin/certificates/" + certNo;
    }

    @GetMapping("/ledger")
    public String ledger(Model model) {
        model.addAttribute("nav", "ledger");
        model.addAttribute("blocks", ledger.findLatest(300));
        model.addAttribute("head", ledger.head());
        return "admin/ledger";
    }

    @GetMapping("/integrity")
    public String integrity(Model model) {
        model.addAttribute("nav", "integrity");
        model.addAttribute("report", integrityService.getLastReport());
        model.addAttribute("trigger", integrityService.getLastTrigger());
        return "admin/integrity";
    }

    @PostMapping("/integrity")
    public String runIntegrity(RedirectAttributes flash) {
        IntegrityReport report = integrityService.runManualAudit();
        flash.addFlashAttribute(report.isIntact() ? "success" : "error", report.summary());
        return "redirect:/admin/integrity";
    }

    @GetMapping("/audit")
    public String audit(Model model) {
        model.addAttribute("nav", "audit");
        model.addAttribute("entries", audit.findLatest(300));
        model.addAttribute("suspicious", verificationLog.findSuspicious(50));
        return "admin/audit";
    }
}
