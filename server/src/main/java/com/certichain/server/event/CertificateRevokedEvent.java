package com.certichain.server.event;

import com.certichain.core.model.Certificate;
import com.certichain.core.model.LedgerBlock;
import org.springframework.context.ApplicationEvent;

public class CertificateRevokedEvent extends ApplicationEvent {

    private final Certificate certificate;
    private final LedgerBlock block;

    public CertificateRevokedEvent(Object source, Certificate certificate, LedgerBlock block) {
        super(source);
        this.certificate = certificate;
        this.block = block;
    }

    public Certificate getCertificate() { return certificate; }
    public LedgerBlock getBlock() { return block; }
}
