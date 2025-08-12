package com.sdlc.pro.txboard.enums;

import java.io.Serializable;

public enum TransactionPhaseStatus implements Serializable {
    COMMITTED("Committed"),
    ROLLED_BACK("Rolled Back"),
    ERRORED("Errored");

    private final String label;

    TransactionPhaseStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
