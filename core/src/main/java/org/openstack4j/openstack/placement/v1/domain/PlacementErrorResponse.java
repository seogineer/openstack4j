package org.openstack4j.openstack.placement.v1.domain;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** {@code {"errors": [{"status": 409, "code": "...", "title": "...", "detail": "...", "request_id": "..."}]}} */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementErrorResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("errors")
    private List<Error> errors;

    public Error first() {
        return errors == null || errors.isEmpty() ? null : errors.get(0);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Error implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("code")
        public String code;
        @JsonProperty("title")
        public String title;
        @JsonProperty("detail")
        public String detail;
        @JsonProperty("request_id")
        public String requestId;
    }
}
