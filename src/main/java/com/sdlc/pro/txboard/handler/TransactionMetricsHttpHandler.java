package com.sdlc.pro.txboard.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.dto.TransactionMetrics;
import com.sdlc.pro.txboard.repository.TransactionLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.HttpRequestHandler;

import java.io.IOException;
import java.io.PrintWriter;

public class TransactionMetricsHttpHandler implements HttpRequestHandler {
    private final ObjectMapper objectMapper;
    private final TransactionLogRepository transactionLogRepository;

    public TransactionMetricsHttpHandler(ObjectMapper objectMapper, TransactionLogRepository transactionLogRepository) {
        this.objectMapper = objectMapper;
        this.transactionLogRepository = transactionLogRepository;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        PrintWriter writer = response.getWriter();

        long totalTransactions = transactionLogRepository.count();
        long committedCount = transactionLogRepository.countByTransactionStatus(TransactionPhaseStatus.COMMITTED);
        long rolledBackCount = totalTransactions - committedCount;
        double successRate = (committedCount * 100.0) / totalTransactions;
        double avgDuration = transactionLogRepository.averageDuration();
        TransactionMetrics metrics = new TransactionMetrics(totalTransactions, committedCount, rolledBackCount, successRate, avgDuration);

        String json = objectMapper.writeValueAsString(metrics);
        writer.write(json);
        writer.flush();
    }
}
