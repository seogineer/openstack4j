package org.openstack4j.openstack.image.v2.domain.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"import-methods": {"value": [...]}}} of {@code GET /v2/info/import}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceImportMethods implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("import-methods")
    private Map<String, Object> methods;

    @SuppressWarnings("unchecked")
    public List<String> getValue() {
        Object value = methods == null ? null : methods.get("value");
        return value instanceof List ? (List<String>) value : Collections.emptyList();
    }
}
