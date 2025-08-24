package com.sdlc.pro.txboard.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdlc.pro.txboard.dto.TransactionChart;
import com.sdlc.pro.txboard.model.DurationDistribution;
import com.sdlc.pro.txboard.repository.TransactionLogRepository;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.HttpRequestHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class TransactionChartHttpHandler implements HttpRequestHandler {
    private final ObjectMapper objectMapper;
    private final TransactionLogRepository transactionLogRepository;

    public TransactionChartHttpHandler(ObjectMapper objectMapper, TransactionLogRepository transactionLogRepository) {
        this.objectMapper = objectMapper;
        this.transactionLogRepository = transactionLogRepository;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        PrintWriter writer = response.getWriter();

        List<DurationDistribution> durationDistributions = transactionLogRepository.getDurationDistributions();
        TransactionChart chartData = new TransactionChart(durationDistributions);

        String json = objectMapper.writeValueAsString(chartData);
        writer.write(json);
        writer.flush();
    }
}
