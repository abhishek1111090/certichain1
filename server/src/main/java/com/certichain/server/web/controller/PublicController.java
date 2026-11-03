package com.certichain.server.web.controller;

import com.certichain.core.chain.VerificationOutcome;
import com.certichain.core.chain.VerificationResult;
import com.certichain.core.exception.CertificateNotFoundException;
import com.certichain.server.config.InstitutionProfile;
import com.certichain.server.repository.LedgerRepository;
import com.certichain.server.service.StatsService;
import com.certichain.server.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/** Pages anyone can open: landing page, verification, printable certificate. */
@Controller
public class PublicController {

    private final VerificationService verificationService;
    private final StatsService statsService;
    private final LedgerRepository ledger;
    private final InstitutionProfile institution;

    public PublicController(VerificationService verificationService, StatsService statsService,
                            LedgerRepository ledger, InstitutionProfile institution) {
        this.verificationService = verificationService;
        this.statsService = statsService;
        this.ledger = ledger;
        this.institution = institution;
    }

    @ModelAttribute("institution")
    public InstitutionProfile institution() {
        return institution;
    }

    @GetMapping("/")
    public String home(Model model) {
        Map<String, Object> stats = statsService.dashboard();
        model.addAttribute("stats", stats);
        model.addAttribute("head", ledger.head());
        return "index";
    }

    @GetMapping("/verify")
    public String verifyForm(@RequestParam(required = false) String certNo) {
        String normalised = VerificationService.normalise(certNo);
        return normalised.isEmpty() ? "redirect:/" : "redirect:/verify/" + normalised;
    }

    @GetMapping("/verify/{certNo}")
    public String verify(@PathVariable String certNo, @RequestParam(required = false) String via,
                         HttpServletRequest request, Model model) {
        VerificationService.Channel channel = "qr".equals(via) ? VerificationService.Channel.QR : VerificationService.Channel.WEB;
        VerificationResult result = verificationService.verify(certNo, channel, request.getRemoteAddr(),
                request.getHeader("User-Agent"));
        model.addAttribute("result", result);
        return "verify";
    }

    @GetMapping("/certificate/{certNo}")
    public String printable(@PathVariable String certNo, HttpServletRequest request, Model model) {
        VerificationResult result = verificationService.verify(certNo, VerificationService.Channel.ADMIN,
                request.getRemoteAddr(), request.getHeader("User-Agent"));
        if (result.getCertificate() == null || result.getOutcome() == VerificationOutcome.NOT_FOUND) {
            throw new CertificateNotFoundException(certNo);
        }
        model.addAttribute("result", result);
        model.addAttribute("c", result.getCertificate());
        return "certificate";
    }
}
