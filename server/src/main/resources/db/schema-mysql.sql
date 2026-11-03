-- MySQL 8 variant of core/src/main/resources/db/schema.sql
CREATE TABLE IF NOT EXISTS admin_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    full_name     VARCHAR(100) NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    BIGINT       NOT NULL
);

CREATE TABLE IF NOT EXISTS certificate (
    cert_no         VARCHAR(30)  PRIMARY KEY,
    student_name    VARCHAR(120) NOT NULL,
    roll_no         VARCHAR(30)  NOT NULL,
    email           VARCHAR(120),
    program         VARCHAR(120) NOT NULL,
    credential_type VARCHAR(30)  NOT NULL,
    title           VARCHAR(200) NOT NULL,
    grade           VARCHAR(20),
    issue_date      DATE         NOT NULL,
    issued_by       VARCHAR(50)  NOT NULL,
    status          VARCHAR(10)  NOT NULL,
    revoke_reason   VARCHAR(255),
    data_hash       CHAR(64)     NOT NULL,
    created_at      BIGINT       NOT NULL
);

CREATE TABLE IF NOT EXISTS ledger_block (
    seq        BIGINT      PRIMARY KEY,
    block_type VARCHAR(10) NOT NULL,
    cert_no    VARCHAR(30) NOT NULL,
    data_hash  CHAR(64)    NOT NULL,
    prev_hash  CHAR(64)    NOT NULL,
    block_hash CHAR(64)    NOT NULL,
    actor      VARCHAR(50) NOT NULL,
    created_at BIGINT      NOT NULL,
    INDEX idx_ledger_cert (cert_no)
);

CREATE TABLE IF NOT EXISTS ledger_head (
    id        INT      PRIMARY KEY,
    last_seq  BIGINT   NOT NULL,
    last_hash CHAR(64) NOT NULL
);

INSERT INTO ledger_head (id, last_seq, last_hash)
    SELECT 1, 0, '0000000000000000000000000000000000000000000000000000000000000000' FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM ledger_head WHERE id = 1);

CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor       VARCHAR(50)  NOT NULL,
    action      VARCHAR(60)  NOT NULL,
    detail      VARCHAR(500),
    outcome     VARCHAR(10)  NOT NULL,
    duration_ms BIGINT       NOT NULL,
    source      VARCHAR(10)  NOT NULL,
    created_at  BIGINT       NOT NULL
);

CREATE TABLE IF NOT EXISTS verification_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    cert_no    VARCHAR(60) NOT NULL,
    result     VARCHAR(20) NOT NULL,
    channel    VARCHAR(10) NOT NULL,
    client_ip  VARCHAR(64),
    user_agent VARCHAR(255),
    created_at BIGINT      NOT NULL
);
