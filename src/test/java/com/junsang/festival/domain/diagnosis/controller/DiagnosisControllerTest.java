package com.junsang.festival.domain.diagnosis.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DiagnosisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsDiagnosisWithKoreanEnums() throws Exception {
        String request = """
                {
                  "festivalName": "원주 여름 음악축제",
                  "areaCode": "51",
                  "signguCode": "51130",
                  "startDate": "2026-08-20",
                  "endDate": "2026-08-23",
                  "festivalType": "문화예술",
                  "scale": "중규모",
                  "recurrenceType": "신규",
                  "festivalAddress": "강원특별자치도 원주시 지정면 소금산로 12",
                  "latitude": 37.3682,
                  "longitude": 127.8166
                }
                """;

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.festivalType").value("문화예술"))
                .andExpect(jsonPath("$.scale").value("중규모"))
                .andExpect(jsonPath("$.recurrenceType").value("신규"));
    }

    @Test
    void rejectsNewDiagnosisWithExistingFestivalContentId() throws Exception {
        String request = """
                {
                  "festivalName": "원주 여름 음악축제",
                  "areaCode": "51",
                  "signguCode": "51130",
                  "startDate": "2026-08-20",
                  "endDate": "2026-08-23",
                  "festivalType": "문화예술",
                  "scale": "중규모",
                  "recurrenceType": "신규",
                  "existingFestivalContentId": "2746930",
                  "festivalAddress": "강원특별자치도 원주시 지정면 소금산로 12",
                  "latitude": 37.3682,
                  "longitude": 127.8166
                }
                """;

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsDashboardStructureEvenWhenExternalApisFail() throws Exception {
        String request = """
                {
                  "festivalName": "원주 여름 음악축제",
                  "areaCode": "51",
                  "signguCode": "51130",
                  "startDate": "2026-08-20",
                  "endDate": "2026-08-23",
                  "festivalType": "CULTURE_ART",
                  "scale": "MEDIUM",
                  "recurrenceType": "NEW",
                  "festivalAddress": "강원특별자치도 원주시 지정면 소금산로 12",
                  "latitude": 37.3682,
                  "longitude": 127.8166
                }
                """;

        String response = mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.festivalType").value("문화예술"))
                .andExpect(jsonPath("$.scale").value("중규모"))
                .andExpect(jsonPath("$.recurrenceType").value("신규"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String reportId = response.replaceFirst(".*\\\"reportId\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(get("/api/v1/reports/{reportId}/dashboard", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(reportId))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.profile").exists())
                .andExpect(jsonPath("$.concentration.dailyConcentrations").isArray())
                .andExpect(jsonPath("$.dataStatuses.length()").value(4))
                .andExpect(jsonPath("$.risks").isArray())
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    @Test
    void acceptsFestivalOutsidePredictionRangeAndReturnsForecastStatus() throws Exception {
        String request = """
                {
                  "festivalName": "범위 초과 축제",
                  "areaCode": "51",
                  "signguCode": "51130",
                  "startDate": "%s",
                  "endDate": "%s",
                  "festivalType": "문화예술",
                  "scale": "중규모",
                  "recurrenceType": "신규",
                  "festivalAddress": "강원특별자치도 원주시 지정면 소금산로 12",
                  "latitude": 37.3682,
                  "longitude": 127.8166
                }
                """.formatted(LocalDate.now().plusDays(31), LocalDate.now().plusDays(33));

        String response = mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String reportId = response.replaceFirst(".*\\\"reportId\\\":\\\"([^\\\"]+)\\\".*", "$1");
        mockMvc.perform(get("/api/v1/reports/{reportId}/dashboard", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataStatuses[?(@.source == 'concentration')].status")
                        .value("OUT_OF_FORECAST_RANGE"));
    }
}
