package com.certichain.server.model;

import java.util.Date;

public record VerificationEntry(String certNo, String result, String channel, String clientIp,
                                String userAgent, long createdAt) {

    public String getCertNo() { return certNo; }
    public String getResult() { return result; }
    public String getChannel() { return channel; }
    public String getClientIp() { return clientIp; }
    public String getUserAgent() { return userAgent; }
    public Date getCreated() { return new Date(createdAt); }
}
