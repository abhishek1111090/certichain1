package com.certichain.server.web.api;

import com.certichain.core.chain.IntegrityReport;
import com.certichain.core.chain.VerificationResult;
import com.certichain.core.model.Certificate;
import com.certichain.server.config.InstitutionProfile;
import com.certichain.server.repository.LedgerRepository;
import com.certichain.server.service.IntegrityService;
import com.certichain.server.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON API so other systems (an employer's HR portal, a background-check agency, another
 * university's admissions service) can verify certificates machine-to-machine.
 */
@RestController
@RequestMapping("/api/v1")
public class VerificationApiController {

    private final VerificationService verificationService;
    private final LedgerRepository ledger;
    private final IntegrityService integrityService;
    private final InstitutionProfile institution;

    public VerificationApiController(VerificationService verificationService, LedgerRepository ledger,
                                     IntegrityService integrityService, InstitutionProfile institution) {
        this.verificationService = verificationService;
        this.ledger = ledger;
        this.integrityService = integrityService;
        this.institution = institution;
    }

    @GetMapping("/verify/{certNo}")
    public ResponseEntity<Map<String, Object>> verify(@PathVariable String certNo, HttpServletRequest request) {
        VerificationResult r = verificationService.verify(certNo, VerificationService.Channel.API,
                request.getRemoteAddr(), request.getHeader("User-Agent"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("certNo", r.getCertNo());
        body.put("outcome", r.getOutcome().name());
        body.put("message", r.getOutcome().getExplanation());
        body.put("issuer", institution.getName());
        Certificate c = r.getCertificate();
        if (c != null && r.getIssueBlock() != null) {
            Map<String, Object> cert = new LinkedHashMap<>();
            cert.put("studentName", c.getStudentName());
            cert.put("rollNo", c.getRollNo());
            cert.put("program", c.getProgram());
            cert.put("credentialType", c.getCredentialType().name());
            cert.put("title", c.getTitle());
            cert.put("grade", c.getGrade());
            cert.put("issueDate", c.getIssueDate().toString());
            body.put("certificate", cert);
            body.put("ledgerBlock", r.getIssueBlock().getSeq());
            body.put("blockHash", r.getIssueBlock().getBlockHash());
        }
        if (r.getRevokeBlock() != null && c != null) {
            body.put("revocationReason", c.getRevokeReason());
        }
        List<Map<String, Object>> checks = r.getChecks().stream()
                .map(ch -> Map.<String, Object>of("check", ch.name(), "passed", ch.passed(), "detail", ch.detail()))
                .toList();
        body.put("checks", checks);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/ledger/head")
    public Map<String, Object> head() {
        LedgerRepository.Head head = ledger.head();
        IntegrityReport last = integrityService.getLastReport();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("issuer", institution.getName());
        body.put("lastSeq", head.seq());
        body.put("lastHash", head.hash());
        body.put("lastAuditIntact", last == null ? null : last.isIntact());
        body.put("lastAuditAt", last == null ? null : last.getFinishedAt());
        return body;
    }
}
