package com.sdlc.pro.txboard.util;

import com.sdlc.pro.txboard.model.*;
import com.sdlc.pro.txboard.enums.IsolationLevel;
import com.sdlc.pro.txboard.enums.PropagationBehavior;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.TransactionLogDocument;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class TransactionLogMapper {

    private TransactionLogMapper() {}

    // ---------------------------------------------------------------------
    // TransactionLog -> TransactionLogDocument
    // ---------------------------------------------------------------------
    public static TransactionLogDocument toDocument(TransactionLog log) {
        if (log == null) return null;

        TransactionLogDocument doc = new TransactionLogDocument();
        doc.setId(log.getTxId() != null ? log.getTxId().toString() : null);
        doc.setTxId(log.getTxId());
        doc.setMethod(log.getMethod());
        doc.setPropagation(log.getPropagation() != null ? log.getPropagation().name() : null);
        doc.setIsolation(log.getIsolation() != null ? log.getIsolation().name() : null);
        doc.setStartTime(log.getStartTime().toEpochMilli());
        doc.setEndTime(log.getEndTime().toEpochMilli());
        doc.setDuration(log.getDuration());
        doc.setStatus(log.getStatus() != null ? log.getStatus().name() : null);
        doc.setThread(log.getThread());
        doc.setConnectionSummary(log.getConnectionSummary());
        doc.setConnectionOriented(Boolean.TRUE.equals(log.getConnectionOriented()) ? log.getConnectionOriented().toString() : "false");
        doc.setAlarmingTransaction(log.isAlarmingTransaction());
        doc.setHavingAlarmingConnection(log.getHavingAlarmingConnection());

        doc.setExecutedQueries(new ArrayList<>(log.getExecutedQuires()));

        doc.setPostTransactionQueries(new ArrayList<>(log.getPostTransactionQuires() == null ? List.of() : log.getPostTransactionQuires()));

        doc.setEvents(log.getEvents() != null ? new ArrayList<>(log.getEvents()) : List.of());

        List<TransactionLogDocument> childDocs = new ArrayList<>();
        for (TransactionLog child : log.getChild()) {
            childDocs.add(toDocument(child));
        }
        doc.setChild(childDocs);

        return doc;
    }

    // ---------------------------------------------------------------------
    // TransactionLogDocument -> TransactionLog
    // ---------------------------------------------------------------------
    public static TransactionLog fromDocument(TransactionLogDocument doc) {
        if (doc == null) return null;

        List<TransactionLog> children = new ArrayList<>();
        if (doc.getChild() != null) {
            for (TransactionLogDocument childDoc : doc.getChild()) {
                children.add(fromDocument(childDoc));
            }
        }

        return new TransactionLog(
                doc.getTxId(),
                doc.getMethod(),
                doc.getPropagation() != null ? PropagationBehavior.valueOf(doc.getPropagation()) : null,
                doc.getIsolation() != null ? IsolationLevel.valueOf(doc.getIsolation()) : null,
                Instant.ofEpochMilli(doc.getStartTime()),
                Instant.ofEpochMilli(doc.getEndTime()),
                doc.getConnectionSummary(),
                doc.getStatus() != null ? TransactionPhaseStatus.valueOf(doc.getStatus()) : null,
                doc.getThread(),
                doc.getExecutedQueries(),
                children,
                doc.getEvents(),
                0,
                doc.getPostTransactionQueries()
        );
    }
}
