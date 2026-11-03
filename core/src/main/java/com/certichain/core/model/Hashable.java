package com.certichain.core.model;

import com.certichain.core.crypto.HashUtil;

/**
 * Anything that can be sealed into the ledger exposes a canonical, order-stable
 * string form. The hash is always computed over this form, never over toString().
 */
public interface Hashable {

    String canonicalForm();

    /** SHA-256 (hex) of {@link #canonicalForm()}. */
    default String computeHash() {
        return HashUtil.sha256Hex(canonicalForm());
    }
}
