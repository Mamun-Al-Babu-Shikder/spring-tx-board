package com.sdlc.pro.txboard.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdlc.pro.txboard.enums.IsolationLevel;
import com.sdlc.pro.txboard.enums.PropagationBehavior;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.domain.*;
import com.sdlc.pro.txboard.repository.TransactionLogRepository;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.HttpRequestHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

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
        String propagation = request.getParameter("propagation");
        String isolation = request.getParameter("isolation");
        String connectionOriented = request.getParameter("connectionOriented");

        FilterNode filter = buildFilter(search, status, propagation, isolation, connectionOriented);
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

    public static FilterNode buildFilter(String search, String status, String propagation, String isolation, String connectionOriented) {
        List<FilterNode> filters = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            filters.add(FilterGroup.of(
                    asList(
                            Filter.of("method", search, Filter.Operator.CONTAINS),
                            Filter.of("thread", search, Filter.Operator.CONTAINS)
                    ),
                    FilterGroup.Logic.OR
            ));
        }

        if (status != null && !status.trim().isEmpty()) {
            try {
                filters.add(Filter.of("status", TransactionPhaseStatus.valueOf(status), Filter.Operator.EQUALS));
            } catch (Exception e) {
                throw new IllegalArgumentException("The value of status must be " + Arrays.toString(TransactionPhaseStatus.values()));
            }
        }

        if (propagation != null && !propagation.trim().isEmpty()) {
            try {
                filters.add(Filter.of("propagation", PropagationBehavior.valueOf(propagation), Filter.Operator.EQUALS));
            } catch (Exception e) {
                throw new IllegalArgumentException("The value of propagation must be " + Arrays.toString(PropagationBehavior.values()));
            }
        }

        if (isolation != null && !isolation.trim().isEmpty()) {
            try {
                filters.add(Filter.of("isolation", IsolationLevel.valueOf(isolation), Filter.Operator.EQUALS));
            } catch (Exception e) {
                throw new IllegalArgumentException("The value of isolation must be " + Arrays.toString(IsolationLevel.values()));
            }
        }

        if (connectionOriented != null && !connectionOriented.trim().isEmpty()) {
            try {
                filters.add(Filter.of("connectionOriented", Boolean.valueOf(connectionOriented), Filter.Operator.EQUALS));
            } catch (Exception e) {
                throw new IllegalArgumentException("The value of connectionOriented must be [true or false]");
            }
        }

        return filters.isEmpty() ? FilterNode.UNFILTERED : FilterGroup.of(filters, FilterGroup.Logic.AND);
    }
}
