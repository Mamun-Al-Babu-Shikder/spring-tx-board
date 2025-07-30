package com.sdlc.pro.txboard.enums;

import java.io.Serializable;

public enum TransactionStatus implements Serializable {
    COMMITTED("Committed"), ROLLED_BACK("Rolled Back");

    private final String label;

    TransactionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
