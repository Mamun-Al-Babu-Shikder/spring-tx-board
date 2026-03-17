package com.sdlc.pro.txboard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        properties = "sdlc.pro.spring.tx.board.storage=in_memory"
)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {
        SpringTxBoardWebConfiguration.class,
        SpringTxBoardWebConfigurationTest.TestConfig.class
})
public class SpringTxBoardWebConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnAlarmingThresholdJsonWithCorrectFormat() throws Exception {
        mockMvc.perform(get("/api/tx-board/config/alarming-threshold"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.transaction").exists())
                .andExpect(jsonPath("$.connection").exists())
                .andExpect(jsonPath("$.transaction").value(1000))
                .andExpect(jsonPath("$.connection").value(1000));
    }

    @Test
    void shouldReturnTxSummaryJsonWithCorrectFormat() throws Exception {
        mockMvc.perform(get("/api/tx-board/tx-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.committedCount").exists())
                .andExpect(jsonPath("$.rolledBackCount").exists())
                .andExpect(jsonPath("$.erroredCount").exists())
                .andExpect(jsonPath("$.totalDuration").exists())
                .andExpect(jsonPath("$.alarmingCount").exists())
                .andExpect(jsonPath("$.connectionAcquisitionCount").exists())
                .andExpect(jsonPath("$.totalConnectionOccupiedTime").exists())
                .andExpect(jsonPath("$.alarmingConnectionCount").exists())
                .andExpect(jsonPath("$.totalTransaction").exists())
                .andExpect(jsonPath("$.averageDuration").exists())
                .andExpect(jsonPath("$.averageConnectionOccupiedTime").exists());
    }

    @Test
    void shouldReturnTxLogJsonWithCorrectFormat() throws Exception {
        mockMvc.perform(get("/api/tx-board/tx-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.size").isNumber())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.page").isNumber())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.first").exists())
                .andExpect(jsonPath("$.first").isBoolean())
                .andExpect(jsonPath("$.last").exists())
                .andExpect(jsonPath("$.last").isBoolean())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void shouldReturnChartJsonCorrectFormat() throws Exception {
        mockMvc.perform(get("/api/tx-board/tx-charts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationDistribution").exists())
                .andExpect(jsonPath("$.durationDistribution").isArray())
                .andExpect(jsonPath("$.durationDistribution[0].range").exists())
                .andExpect(jsonPath("$.durationDistribution[0].range").isMap())
                .andExpect(jsonPath("$.durationDistribution[0].range.minMillis").exists())
                .andExpect(jsonPath("$.durationDistribution[0].range.maxMillis").exists())
                .andExpect(jsonPath("$.durationDistribution[0].count").exists());
    }

    @Test
    void shouldRedirectToTxBoardHtmlPage() throws Exception {
        mockMvc.perform(get("/tx-board/ui"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tx-board/ui/index.html"));
    }

    @Test
    void shouldReturnTxBoardHtmlPage() throws Exception {
        mockMvc.perform(get("/tx-board/ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML_VALUE));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        TxBoardProperties txBoardProperties() {
            return new TxBoardProperties();
        }
    }
}
