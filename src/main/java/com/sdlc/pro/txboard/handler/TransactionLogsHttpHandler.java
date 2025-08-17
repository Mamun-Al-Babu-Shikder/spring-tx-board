package com.sdlc.pro.txboard.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.domain.*;
import com.sdlc.pro.txboard.repository.TransactionLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.HttpRequestHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransactionLogsHttpHandler implements HttpRequestHandler {
    private final ObjectMapper objectMapper;
    private final TransactionLogRepository transactionLogRepository;

    public TransactionLogsHttpHandler(ObjectMapper objectMapper, TransactionLogRepository transactionLogRepository) {
        this.objectMapper = objectMapper;
        this.transactionLogRepository = transactionLogRepository;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int page = parseIntOrDefault(request.getParameter("page"), 0);
        int size = parseIntOrDefault(request.getParameter("size"), 10);
        Sort sort = parseSort(request.getParameter("sort"));
        String search = request.getParameter("search");
        String status = request.getParameter("status");

        FilterNode filter = buildFilter(search, status);
        TransactionLogPageResponse pageResponse = transactionLogRepository.findAll(
                TransactionLogPageRequest.of(page, size, sort, filter)
        );

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        PrintWriter writer = response.getWriter();

        String json = objectMapper.writeValueAsString(pageResponse);
        writer.write(json);
        writer.flush();
    }

    private int parseIntOrDefault(String param, int defaultValue) {
        try {
            return param != null ? Integer.parseInt(param) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Sort parseSort(String sort) {
        return sort != null ? Sort.from(sort) : Sort.UNSORTED;
    }

    public static FilterNode buildFilter(String search, String status) {
        List<FilterNode> filters = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            filters.add(FilterGroup.of(
                    List.of(
                            Filter.of("method", search, Filter.Operator.CONTAINS),
                            Filter.of("thread", search, Filter.Operator.CONTAINS)
                    ),
                    FilterGroup.Logic.OR
            ));
        }

        if (status != null && !status.isBlank()) {
            try {
                filters.add(Filter.of("status", TransactionPhaseStatus.valueOf(status), Filter.Operator.EQUALS));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("The value of status must be " + Arrays.toString(TransactionPhaseStatus.values()));
            }
        }

        return filters.isEmpty() ? FilterNode.UNFILTERED : FilterGroup.of(filters, FilterGroup.Logic.AND);
    }
}
