package com.isspredictor.iss_predictor_service.controller;

import com.isspredictor.iss_predictor_service.model.*;
import com.isspredictor.iss_predictor_service.service.VisibilityAssessmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer-only test - VisibilityAssessmentService is mocked, so this never
 * makes a real call to any upstream API. Confirms the controller's own job:
 * request binding, validation triggering, and HATEOAS link construction.
 */
@WebMvcTest(PredictionController.class)
class PredictionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VisibilityAssessmentService assessmentService;

    private VisibilityAssessment sampleAssessment() {
        return VisibilityAssessment.builder()
                .location(new Coordinates(12.97, 77.59))
                .currentPosition(IssPosition.builder()
                        .latitude(25.4).longitude(88.1)
                        .altitudeKm(408.0).velocityKmh(27600.0)
                        .observedAt(Instant.now())
                        .freshness(DataFreshness.LIVE)
                        .build())
                .upcomingPasses(List.of())
                .alerts(List.of())
                .overallFreshness(DataFreshness.LIVE)
                .build();
    }

    @Test
    void validCoordinatesReturn200WithAssessmentBody() throws Exception {
        when(assessmentService.assess(any())).thenReturn(sampleAssessment());

        mockMvc.perform(post("/api/v1/predictions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"latitude": 12.97, "longitude": 77.59}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPosition.freshness").value("LIVE"))
                .andExpect(jsonPath("$.currentPosition.altitudeKm").value(408.0));
    }

    @Test
    void responseIncludesHateoasLinks() throws Exception {
        when(assessmentService.assess(any())).thenReturn(sampleAssessment());

        mockMvc.perform(post("/api/v1/predictions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"latitude": 12.97, "longitude": 77.59}
                                """))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.refresh.href").exists());
    }

    @Test
    void latitudeAboveNinetyReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/predictions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"latitude": 91.0, "longitude": 77.59}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("latitude: Latitude must be <= 90"));
    }

    @Test
    void longitudeBelowNegative180Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/predictions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"latitude": 12.97, "longitude": -181.0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("longitude: Longitude must be >= -180"));
    }

    @Test
    void missingLatitudeReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/predictions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"longitude": 77.59}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/predictions")
                        .contentType(APPLICATION_JSON)
                        .content("not valid json"))
                .andExpect(status().isBadRequest());
    }
}