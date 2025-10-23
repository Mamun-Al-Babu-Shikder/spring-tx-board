package com.sdlc.pro.txboard.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdlc.pro.txboard.config.TxBoardProperties;
import org.springframework.http.MediaType;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class AlarmingThresholdHttpHandler implements HttpRequestHandler {
    private final ObjectMapper objectMapper;
    private final TxBoardProperties txBoardProperties;

    public AlarmingThresholdHttpHandler(ObjectMapper objectMapper, TxBoardProperties txBoardProperties) {
        this.objectMapper = objectMapper;
        this.txBoardProperties = txBoardProperties;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        PrintWriter writer = response.getWriter();
        String json = objectMapper.writeValueAsString(txBoardProperties.getAlarmingThreshold());
        writer.write(json);
        writer.flush();
    }
}
