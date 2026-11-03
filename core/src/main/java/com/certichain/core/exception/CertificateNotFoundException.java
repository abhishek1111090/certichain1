package com.certichain.core.exception;

public class CertificateNotFoundException extends CertiChainException {

    private final String certNo;

    public CertificateNotFoundException(String certNo) {
        super("No certificate with number " + certNo);
        this.certNo = certNo;
    }

    public String getCertNo() {
        return certNo;
    }
}
