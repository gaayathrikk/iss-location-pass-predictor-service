/**
 * REST endpoints (thin layer -- delegates to service, no business logic here).
 */
package com.isspredictor.iss_predictor_service.controller;

import com.isspredictor.iss_predictor_service.model.Coordinates;
import com.isspredictor.iss_predictor_service.model.HateoasResponse;
import com.isspredictor.iss_predictor_service.model.VisibilityAssessment;
import com.isspredictor.iss_predictor_service.service.VisibilityAssessmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sole entry point for the ISS visibility prediction API.
 * Thin by design - all real work happens in {@link VisibilityAssessmentService};
 * this class only handles HTTP binding, validation triggering, and HATEOAS links.
 */
@RestController
@RequestMapping("/api/v1")
public class PredictionController {

    private final VisibilityAssessmentService assessmentService;

    public PredictionController(VisibilityAssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @PostMapping("/predictions")
    public ResponseEntity<HateoasResponse<VisibilityAssessment>> getPredictions(
            @Valid @RequestBody Coordinates coordinates) {

        VisibilityAssessment assessment = assessmentService.assess(coordinates);

        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().toUriString();
        Map<String, HateoasResponse.Link> links = new LinkedHashMap<>();
        links.put("self", new HateoasResponse.Link(baseUrl));
        links.put("refresh", new HateoasResponse.Link(baseUrl + "?force=true"));

        return ResponseEntity.ok(new HateoasResponse<>(assessment, links));
    }
}