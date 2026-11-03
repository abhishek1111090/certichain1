package com.certichain.server.service;

import com.certichain.core.exception.CertificateNotFoundException;
import com.certichain.core.exception.IllegalStateTransitionException;
import com.certichain.core.exception.InvalidCertificateException;
import com.certichain.core.model.BlockType;
import com.certichain.core.model.Certificate;
import com.certichain.core.model.CertificateStatus;
import com.certichain.core.model.LedgerBlock;
import com.certichain.core.util.CertNoGenerator;
import com.certichain.core.util.CertificateValidator;
import com.certichain.server.aop.Audited;
import com.certichain.server.config.InstitutionProfile;
import com.certichain.server.event.CertificateIssuedEvent;
import com.certichain.server.event.CertificateRevokedEvent;
import com.certichain.server.repository.CertificateRepository;
import com.certichain.server.repository.LedgerRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Issuing and revoking. Each operation writes the certificate row AND a ledger block in
 * one database transaction: either both are stored or neither is.
 */
@Service
public class CertificateService {

    private final CertificateRepository certificates;
    private final LedgerRepository ledger;
    private final ApplicationEventPublisher events;
    private final InstitutionProfile institution;

    public CertificateService(CertificateRepository certificates, LedgerRepository ledger,
                              ApplicationEventPublisher events, InstitutionProfile institution) {
        this.certificates = certificates;
        this.ledger = ledger;
        this.events = events;
        this.institution = institution;
    }

    @Audited("ISSUE_CERTIFICATE")
    @Transactional
    public Certificate issue(Certificate draft, String actor) {
        return doIssue(draft, actor, null);
    }

    /** Issues under a chosen number; used only for the fixed demo data. */
    @Audited("ISSUE_CERTIFICATE")
    @Transactional
    public Certificate issueWithNumber(Certificate draft, String actor, String certNo) {
        if (certificates.exists(certNo)) {
            throw new InvalidCertificateException(List.of("Certificate number " + certNo + " is already used"));
        }
        return doIssue(draft, actor, certNo);
    }

    private Certificate doIssue(Certificate draft, String actor, String presetNo) {
        CertificateValidator.validate(draft);
        String certNo = presetNo;
        while (certNo == null || (presetNo == null && certificates.exists(certNo))) {
            certNo = CertNoGenerator.next(institution.getShortCode());
        }

        draft.setCertNo(certNo);
        draft.setIssuedBy(actor);
        draft.setStatus(CertificateStatus.VALID);
        draft.setRevokeReason(null);
        normalise(draft);
        draft.setDataHash(draft.computeHash());
        draft.setCreatedAt(System.currentTimeMillis());

        certificates.insert(draft);
        LedgerBlock block = ledger.append(BlockType.ISSUE, certNo, draft.getDataHash(), actor);
        events.publishEvent(new CertificateIssuedEvent(this, draft, block));
        return draft;
    }

    @Audited("REVOKE_CERTIFICATE")
    @Transactional
    public Certificate revoke(String certNo, String reason, String actor) {
        if (reason == null || reason.isBlank() || reason.trim().length() < 5) {
            throw new InvalidCertificateException(List.of("Give a revocation reason of at least 5 characters"));
        }
        Certificate c = certificates.findByCertNo(certNo).orElseThrow(() -> new CertificateNotFoundException(certNo));
        if (c.isRevoked() || ledger.findByCertNo(certNo).stream().anyMatch(b -> b.getType() == BlockType.REVOKE)) {
            throw new IllegalStateTransitionException(certNo + " is already revoked");
        }
        c.setStatus(CertificateStatus.REVOKED);
        c.setRevokeReason(reason.trim());
        certificates.markRevoked(certNo, c.getRevokeReason());
        LedgerBlock block = ledger.append(BlockType.REVOKE, certNo, c.revocationHash(), actor);
        events.publishEvent(new CertificateRevokedEvent(this, c, block));
        return c;
    }

    public Certificate get(String certNo) {
        return certificates.findByCertNo(certNo).orElseThrow(() -> new CertificateNotFoundException(certNo));
    }

    public List<Certificate> search(String q, String status, String type) {
        return certificates.search(q, status, type, 500);
    }

    public List<Certificate> all() {
        return certificates.findAll();
    }

    /** Trim text so that what is hashed is exactly what is stored. */
    private static void normalise(Certificate c) {
        c.setStudentName(c.getStudentName().trim().replaceAll("\\s+", " "));
        c.setRollNo(c.getRollNo().trim().toUpperCase());
        c.setEmail(c.getEmail() == null || c.getEmail().isBlank() ? null : c.getEmail().trim().toLowerCase());
        c.setProgram(c.getProgram().trim());
        c.setTitle(c.getTitle().trim());
        c.setGrade(c.getGrade() == null || c.getGrade().isBlank() ? null : c.getGrade().trim());
    }
}
