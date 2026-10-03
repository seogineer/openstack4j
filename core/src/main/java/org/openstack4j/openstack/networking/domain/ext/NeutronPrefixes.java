package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"prefixes": [...]}}: request and response of the subnet pool prefix operations. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronPrefixes implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("prefixes")
    private List<String> prefixes;

    public List<String> getPrefixes() {
        return prefixes;
    }
}
