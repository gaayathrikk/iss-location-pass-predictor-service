package com.isspredictor.iss_predictor_service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.Map;

/**
 * Wraps any response body with a {@code _links} map, per HATEOAS.
 * <p>
 * Kept as a generic wrapper rather than adding a links field directly to
 * {@link VisibilityAssessment} - the domain model shouldn't know about HTTP
 * navigation concerns; that's a presentation-layer decision made at the
 * controller boundary.
 */
public record HateoasResponse<T>(
        @JsonUnwrapped T body,
        @JsonProperty("_links") Map<String, Link> links
) {
    public record Link(String href) {
    }
}