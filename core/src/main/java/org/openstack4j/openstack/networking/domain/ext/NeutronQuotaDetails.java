package org.openstack4j.openstack.networking.domain.ext;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.network.ext.QuotaDetail;

/** {@code {"quota": {"<resource>": {"limit", "used", "reserved"}}}} of {@code GET /quotas/{id}/details.json}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronQuotaDetails implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("quota")
    private Map<String, Detail> quota;

    public Map<String, Detail> getQuota() {
        return quota;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Detail implements QuotaDetail {

        private static final long serialVersionUID = 1L;

        @JsonProperty("limit") private Integer limit;
        @JsonProperty("used") private Integer used;
        @JsonProperty("reserved") private Integer reserved;

        @Override public Integer getLimit() { return limit; }
        @Override public Integer getUsed() { return used; }
        @Override public Integer getReserved() { return reserved; }
    }
}
