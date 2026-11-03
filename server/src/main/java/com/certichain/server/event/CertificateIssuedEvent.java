package com.certichain.server.event;

import com.certichain.core.model.Certificate;
import com.certichain.core.model.LedgerBlock;
import org.springframework.context.ApplicationEvent;

/** Published inside the issuing transaction; listeners run after it commits. */
public class CertificateIssuedEvent extends ApplicationEvent {

    private final Certificate certificate;
    private final LedgerBlock block;

    public CertificateIssuedEvent(Object source, Certificate certificate, LedgerBlock block) {
        super(source);
        this.certificate = certificate;
        this.block = block;
    }

    public Certificate getCertificate() { return certificate; }
    public LedgerBlock getBlock() { return block; }
}
