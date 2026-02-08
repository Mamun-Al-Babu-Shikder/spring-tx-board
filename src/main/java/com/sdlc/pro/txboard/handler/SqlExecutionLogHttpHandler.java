package com.sdlc.pro.txboard.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdlc.pro.txboard.domain.*;
import com.sdlc.pro.txboard.model.SqlExecutionLog;
import com.sdlc.pro.txboard.repository.SqlExecutionLogRepository;
import org.springframework.http.MediaType;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

public class SqlExecutionLogHttpHandler implements HttpRequestHandler {
    private final ObjectMapper objectMapper;
    private final SqlExecutionLogRepository sqlExecutionLogRepository;

    public SqlExecutionLogHttpHandler(ObjectMapper objectMapper, SqlExecutionLogRepository sqlExecutionLogRepository) {
        this.objectMapper = objectMapper;
        this.sqlExecutionLogRepository = sqlExecutionLogRepository;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int page = parseIntOrDefault(request.getParameter("page"), 0);
        int size = parseIntOrDefault(request.getParameter("size"), 10);
        Sort sort = parseSort(request.getParameter("sort"));
        String search = request.getParameter("search");

        if (page < 0) {
            throw new IllegalArgumentException("The value of 'page' must be positive integer");
        }

        if (size < 1 || size > 1000) {
            throw new IllegalArgumentException("The value of 'size' must be between 1 to 1000");
        }

        FilterNode filter = search != null && !search.trim().isEmpty() ? FilterGroup.of(
                Collections.singletonList(Filter.of("thread", search, Filter.Operator.CONTAINS)),
                FilterGroup.Logic.OR
        ) : FilterNode.UNFILTERED;

        PageResponse<SqlExecutionLog> pageResponse = this.sqlExecutionLogRepository.findAll(
                PageRequest.of(page, size, sort, filter));

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
}
